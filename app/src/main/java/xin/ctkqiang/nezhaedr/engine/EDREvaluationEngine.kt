package xin.ctkqiang.nezhaedr.engine

import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent

data class RuleMatchResult(
    val rule: DetectionRule,
    val event: TelemetryEvent
)

class EDREvaluationEngine {

    private var activeRules: List<DetectionRule> = emptyList()
    private val compiledPackageRegexes = mutableListOf<Pair<DetectionRule, List<Regex>>>()
    private val ipLookupTable = mutableMapOf<String, DetectionRule>()
    private val domainLookupTable = mutableMapOf<String, DetectionRule>()

    fun updateRules(rulePackage: DetectionRulePackage) {
        activeRules = rulePackage.rules
        ipLookupTable.clear()
        domainLookupTable.clear()
        compiledPackageRegexes.clear()

        for (rule in activeRules) {
            for (ip in rule.condition.targetIps) {
                ipLookupTable[ip] = rule
            }

            for (domain in rule.condition.targetDomains) {
                val cleanDomain = domain.lowercase().trim()
                domainLookupTable[cleanDomain] = rule
            }

            if (rule.condition.packagePatterns.isNotEmpty()) {
                val regexes = rule.condition.packagePatterns.mapNotNull { pattern ->
                    try {
                        Regex(pattern, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (regexes.isNotEmpty()) {
                    compiledPackageRegexes.add(Pair(rule, regexes))
                }
            }
        }
    }

    fun evaluateNetworkEvent(event: TelemetryEvent.Network): RuleMatchResult? {
        val matchedIpRule = ipLookupTable[event.destinationIp]
        if (matchedIpRule != null) {
            return RuleMatchResult(rule = matchedIpRule, event = event)
        }

        val targetDomain = event.destinationIp.lowercase().trim()
        val matchedDomainRule = domainLookupTable[targetDomain]
        if (matchedDomainRule != null) {
            return RuleMatchResult(rule = matchedDomainRule, event = event)
        }

        // Subdomain wildcard matching (e.g. www.toppornsites.com -> toppornsites.com)
        for ((domainPattern, rule) in domainLookupTable) {
            if (targetDomain.endsWith(".$domainPattern") || targetDomain == domainPattern) {
                return RuleMatchResult(rule = rule, event = event)
            }
        }

        return null
    }

    fun evaluatePackageEvent(event: TelemetryEvent.Package): RuleMatchResult? {
        for ((rule, regexList) in compiledPackageRegexes) {
            for (regex in regexList) {
                if (regex.matches(event.packageName)) {
                    return RuleMatchResult(rule = rule, event = event)
                }
            }
        }
        return null
    }
}
