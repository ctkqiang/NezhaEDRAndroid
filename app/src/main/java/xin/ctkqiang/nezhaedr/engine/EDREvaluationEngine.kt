package xin.ctkqiang.nezhaedr.engine

import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent
import xin.ctkqiang.nezhaedr.util.EDRLogger

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
        activeRules = rulePackage.rules
        ipLookupTable.clear()
        compiledPackageRegexes.clear()

        for (rule in activeRules) {
            EDRLogger.v(TAG, "Indexing rule: ${rule.id} (${rule.name})")
            for (ip in rule.condition.targetIps) {
                ipLookupTable[ip] = rule
                EDRLogger.v(TAG, "Indexed target IP '$ip' -> Rule ${rule.id}")
            }

            if (rule.condition.packagePatterns.isNotEmpty()) {
                val regexes = rule.condition.packagePatterns.mapNotNull { pattern ->
                    try {
                        Regex(pattern, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        EDRLogger.w(TAG, "Failed to compile Regex pattern '$pattern' in rule ${rule.id}", e)
                        null
                    }
                }
                if (regexes.isNotEmpty()) {
                    compiledPackageRegexes.add(Pair(rule, regexes))
                    EDRLogger.v(TAG, "Compiled ${regexes.size} Regex package patterns for rule ${rule.id}")
                }
            }
        }
        EDRLogger.d(TAG, "updateRules complete: ${ipLookupTable.size} target IPs indexed, ${compiledPackageRegexes.size} package Regex sets compiled")
    }

    fun evaluateNetworkEvent(event: TelemetryEvent.Network): RuleMatchResult? {
        EDRLogger.v(TAG, "evaluateNetworkEvent: Evaluating destination '${event.destinationIp}:${event.destinationPort}', Domain: '${event.domain}' (${event.protocol})")

        // O(1) Target IP Match
        val matchedRule = ipLookupTable[event.destinationIp]
        if (matchedRule != null) {
            EDRLogger.w(TAG, "MATCH FOUND (IP): Destination IP '${event.destinationIp}' matched Rule ${matchedRule.id} (${matchedRule.name}) [Action: ${matchedRule.action}]")
            return RuleMatchResult(rule = matchedRule, event = event)
        }

        // Target Domain & IP Substring Match
        val destLower = event.destinationIp.lowercase()
        val domainLower = event.domain.lowercase()

        for (rule in activeRules) {
            for (targetDomain in rule.condition.targetDomains) {
                val targetLower = targetDomain.lowercase()
                val domainMatched = domainLower.isNotEmpty() && (domainLower == targetLower || domainLower.endsWith(".$targetLower") || domainLower.contains(targetLower))
                val ipMatched = destLower.isNotEmpty() && destLower.contains(targetLower)

                if (domainMatched || ipMatched) {
                    EDRLogger.w(TAG, "MATCH FOUND (Domain): Destination Domain/IP '${event.domain}'/'${event.destinationIp}' matched target domain '$targetDomain' in Rule ${rule.id} (${rule.name}) [Action: ${rule.action}]")
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
                    EDRLogger.w(TAG, "MATCH FOUND (Package): Package '${event.packageName}' matched Regex '${regex.pattern}' in Rule ${rule.id} (${rule.name}) [Action: ${rule.action}]")
                    return RuleMatchResult(rule = rule, event = event)
                }
            }
        }
        EDRLogger.v(TAG, "evaluatePackageEvent: No match for '${event.packageName}' (CLEAN)")
        return null
    }

    companion object {
        private const val TAG = "EDREvaluationEngine"
    }
}
