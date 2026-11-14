package xin.ctkqiang.nezhaedr.engine

import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent
import xin.ctkqiang.nezhaedr.util.EDRLogger
import kotlin.math.log2

data class RuleMatchResult(
    val rule: DetectionRule,
    val event: TelemetryEvent
)

class EDREvaluationEngine {

    private var activeRules: List<DetectionRule> = emptyList()
    private val compiledPackageRegexes = mutableListOf<Pair<DetectionRule, List<Regex>>>()
    private val ipLookupTable = mutableMapOf<String, DetectionRule>()

    fun updateRules(rulePackage: DetectionRulePackage) {
        EDRLogger.i(TAG, "updateRules: Loading rule package version ${rulePackage.version} with ${rulePackage.rules.size} rules")
        activeRules = rulePackage.rules.filter { it.enabled }
        ipLookupTable.clear()
        compiledPackageRegexes.clear()

        for (rule in activeRules) {
            EDRLogger.v(TAG, "Indexing rule: ${rule.canonicalId} (${rule.name}) [Engine: ${rule.engineType}]")
            for (ip in rule.condition.targetIps) {
                ipLookupTable[ip] = rule
                EDRLogger.v(TAG, "Indexed target IP '$ip' -> Rule ${rule.canonicalId}")
            }

            if (rule.condition.packagePatterns.isNotEmpty()) {
                val regexes = rule.condition.packagePatterns.mapNotNull { pattern ->
                    try {
                        Regex(pattern, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        EDRLogger.w(TAG, "Failed to compile Regex pattern '$pattern' in rule ${rule.canonicalId}", e)
                        null
                    }
                }
                if (regexes.isNotEmpty()) {
                    compiledPackageRegexes.add(Pair(rule, regexes))
                    EDRLogger.v(TAG, "Compiled ${regexes.size} Regex package patterns for rule ${rule.canonicalId}")
                }
            }
        }
        EDRLogger.d(TAG, "updateRules complete: ${ipLookupTable.size} target IPs indexed, ${compiledPackageRegexes.size} package Regex sets compiled")
    }

    fun evaluateNetworkEvent(event: TelemetryEvent.Network): RuleMatchResult? {
        EDRLogger.v(TAG, "evaluateNetworkEvent: Evaluating destination '${event.destinationIp}:${event.destinationPort}', Domain: '${event.domain}' (${event.protocol})")

        // 1. Signature Engine: O(1) Target IP Match
        val matchedIpRule = ipLookupTable[event.destinationIp]
        if (matchedIpRule != null) {
            EDRLogger.w(TAG, "MATCH FOUND (IP): Destination IP '${event.destinationIp}' matched Rule ${matchedIpRule.canonicalId} (${matchedIpRule.name}) [Action: ${matchedIpRule.action}]")
            return RuleMatchResult(rule = matchedIpRule, event = event)
        }

        val destLower = event.destinationIp.lowercase()
        val domainLower = event.domain.lowercase()

        for (rule in activeRules) {
            // 2. Signature Engine: Target Domain Substring Match
            for (targetDomain in rule.condition.targetDomains) {
                val targetLower = targetDomain.lowercase()
                val domainMatched = domainLower.isNotEmpty() && (domainLower == targetLower || domainLower.endsWith(".$targetLower") || domainLower.contains(targetLower))
                val ipMatched = destLower.isNotEmpty() && destLower.contains(targetLower)

                if (domainMatched || ipMatched) {
                    EDRLogger.w(TAG, "MATCH FOUND (Domain): Destination Domain/IP '${event.domain}'/'${event.destinationIp}' matched target domain '$targetDomain' in Rule ${rule.canonicalId} (${rule.name}) [Action: ${rule.action}]")
                    return RuleMatchResult(rule = rule, event = event)
                }
            }

            // 3. Behavioral Engine: Subdomain Depth & Shannon Entropy Evaluation
            if (rule.engineType.equals("behavioral", ignoreCase = true) && domainLower.isNotEmpty()) {
                val maxDepth = rule.condition.maxSubdomainDepth
                if (maxDepth != null) {
                    val parts = domainLower.split(".")
                    val subdomainDepth = if (parts.size > 2) parts.size - 2 else 0
                    if (subdomainDepth > maxDepth) {
                        EDRLogger.w(TAG, "MATCH FOUND (Behavioral Subdomain Depth): Domain '$domainLower' depth $subdomainDepth exceeded limit $maxDepth in Rule ${rule.canonicalId}")
                        return RuleMatchResult(rule = rule, event = event)
                    }
                }

                val maxEntropy = rule.condition.maxEntropy
                if (maxEntropy != null) {
                    val entropy = calculateShannonEntropy(domainLower)
                    if (entropy > maxEntropy) {
                        EDRLogger.w(TAG, "MATCH FOUND (Behavioral Entropy): Domain '$domainLower' Shannon Entropy $entropy exceeded limit $maxEntropy in Rule ${rule.canonicalId}")
                        return RuleMatchResult(rule = rule, event = event)
                    }
                }
            }

            // 4. Conditional Engine: Expression Helper Functions
            if (rule.engineType.equals("conditional", ignoreCase = true) && !rule.condition.expression.isNullOrBlank()) {
                val expr = rule.condition.expression.lowercase()
                val isMatched = evaluateExpression(expr, domainLower, event.destinationIp, event.destinationPort)
                if (isMatched) {
                    EDRLogger.w(TAG, "MATCH FOUND (Conditional Expression): Event matched expression '$expr' in Rule ${rule.canonicalId}")
                    return RuleMatchResult(rule = rule, event = event)
                }
            }
        }

        EDRLogger.v(TAG, "evaluateNetworkEvent: No match for '${event.destinationIp}' / '${event.domain}' (CLEAN)")
        return null
    }

    fun evaluatePackageEvent(event: TelemetryEvent.Package): RuleMatchResult? {
        EDRLogger.d(TAG, "evaluatePackageEvent: Evaluating package '${event.packageName}' (Action: ${event.action})")
        for ((rule, regexList) in compiledPackageRegexes) {
            for (regex in regexList) {
                if (regex.matches(event.packageName)) {
                    EDRLogger.w(TAG, "MATCH FOUND (Package): Package '${event.packageName}' matched Regex '${regex.pattern}' in Rule ${rule.canonicalId} (${rule.name}) [Action: ${rule.action}]")
                    return RuleMatchResult(rule = rule, event = event)
                }
            }
        }
        EDRLogger.v(TAG, "evaluatePackageEvent: No match for '${event.packageName}' (CLEAN)")
        return null
    }

    private fun evaluateExpression(expr: String, domain: String, ip: String, port: Int): Boolean {
        return try {
            if (expr.contains("entropy(domain)")) {
                val entropy = calculateShannonEntropy(domain)
                if (expr.contains(">")) {
                    val threshold = expr.substringAfter(">").trim().toDoubleOrNull() ?: 4.0
                    return entropy > threshold
                }
            }
            if (expr.contains("contains(domain,")) {
                val arg = expr.substringAfter("contains(domain,").substringBefore(")").trim().removeSurrounding("\"")
                if (domain.contains(arg)) return true
            }
            if (expr.contains("endswith(domain,")) {
                val arg = expr.substringAfter("endswith(domain,").substringBefore(")").trim().removeSurrounding("\"")
                if (domain.endsWith(arg)) return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "EDREvaluationEngine"

        fun calculateShannonEntropy(s: String): Double {
            if (s.isEmpty()) return 0.0
            val charCounts = mutableMapOf<Char, Int>()
            for (c in s) charCounts[c] = (charCounts[c] ?: 0) + 1
            var entropy = 0.0
            val len = s.length.toDouble()
            for (count in charCounts.values) {
                val p = count / len
                entropy -= p * log2(p)
            }
            return entropy
        }
    }
}
