package xin.ctkqiang.nezhaedr.data.parser

import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.RuleCondition
import xin.ctkqiang.nezhaedr.util.EDRLogger

object YamlRuleParser {

    private const val TAG = "YamlRuleParser"

    fun parseYamlRule(yamlText: String): DetectionRule? {
        EDRLogger.v(TAG, "Parsing YAML rule snippet (${yamlText.length} bytes)")
        val lines = yamlText.lines()
        var id = ""
        var ruleId = ""
        var schemaVersion = "2.0"
        var name = ""
        var description = ""
        var severity = "MEDIUM"
        var category = "NETWORK"
        var enabled = true
        var engineType = "signature"
        var targetSource = "dns_query"
        var actionStr = "LOG"

        val targetIps = mutableListOf<String>()
        val targetDomains = mutableListOf<String>()
        val targetPorts = mutableListOf<Int>()
        val packagePatterns = mutableListOf<String>()
        val allowedPackages = mutableListOf<String>()
        var maxSubdomainDepth: Int? = null
        var maxEntropy: Double? = null
        var expression: String? = null

        var currentSection = ""

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.startsWith("rule_id:")) {
                ruleId = trimmed.substringAfter("rule_id:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("id:")) {
                id = trimmed.substringAfter("id:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("schema_version:")) {
                schemaVersion = trimmed.substringAfter("schema_version:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("name:")) {
                name = trimmed.substringAfter("name:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("description:")) {
                description = trimmed.substringAfter("description:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("severity:")) {
                severity = trimmed.substringAfter("severity:").trim().removeSurrounding("\"").uppercase()
            } else if (trimmed.startsWith("category:")) {
                category = trimmed.substringAfter("category:").trim().removeSurrounding("\"").uppercase()
            } else if (trimmed.startsWith("enabled:")) {
                enabled = trimmed.substringAfter("enabled:").trim().toBooleanStrictOrNull() ?: true
            } else if (trimmed.startsWith("engine_type:")) {
                engineType = trimmed.substringAfter("engine_type:").trim().removeSurrounding("\"").lowercase()
            } else if (trimmed.startsWith("target_source:")) {
                targetSource = trimmed.substringAfter("target_source:").trim().removeSurrounding("\"").lowercase()
            } else if (trimmed.startsWith("action:")) {
                actionStr = trimmed.substringAfter("action:").trim().removeSurrounding("\"").uppercase()
            } else if (trimmed == "condition:") {
                currentSection = "condition"
            } else if (trimmed.startsWith("max_subdomain_depth:")) {
                maxSubdomainDepth = trimmed.substringAfter("max_subdomain_depth:").trim().toIntOrNull()
            } else if (trimmed.startsWith("max_entropy:")) {
                maxEntropy = trimmed.substringAfter("max_entropy:").trim().toDoubleOrNull()
            } else if (trimmed.startsWith("expression:")) {
                expression = trimmed.substringAfter("expression:").trim().removeSurrounding("\"")
            } else if (trimmed == "target_ips:") {
                currentSection = "target_ips"
            } else if (trimmed == "target_domains:") {
                currentSection = "target_domains"
            } else if (trimmed == "target_ports:") {
                currentSection = "target_ports"
            } else if (trimmed == "package_patterns:") {
                currentSection = "package_patterns"
            } else if (trimmed == "allowed_packages:") {
                currentSection = "allowed_packages"
            } else if (trimmed.startsWith("-")) {
                val item = trimmed.substringAfter("-").trim().removeSurrounding("\"")
                when (currentSection) {
                    "target_ips" -> targetIps.add(item)
                    "target_domains" -> targetDomains.add(item)
                    "target_ports" -> item.toIntOrNull()?.let { targetPorts.add(it) }
                    "package_patterns" -> packagePatterns.add(item)
                    "allowed_packages" -> allowedPackages.add(item)
                }
            }
        }

        // If explicitly disabled, skip parsing rule
        if (!enabled) {
            EDRLogger.i(TAG, "Skipping disabled rule snippet (rule_id: '$ruleId', id: '$id')")
            return null
        }

        val canonicalId = ruleId.ifEmpty { id }
        if (canonicalId.isEmpty() || name.isEmpty()) {
            EDRLogger.w(TAG, "Failed to parse YAML rule: missing required rule_id/id or name field")
            return null
        }

        val containmentAction = try {
            ContainmentAction.valueOf(actionStr)
        } catch (e: Exception) {
            EDRLogger.w(TAG, "Invalid action '$actionStr' for rule $canonicalId, defaulting to LOG", e)
            ContainmentAction.LOG
        }

        val rule = DetectionRule(
            id = canonicalId,
            schemaVersion = schemaVersion,
            ruleId = canonicalId,
            name = name,
            description = description,
            severity = severity,
            category = category,
            enabled = enabled,
            engineType = engineType,
            targetSource = targetSource,
            condition = RuleCondition(
                targetIps = targetIps,
                targetDomains = targetDomains,
                targetPorts = targetPorts,
                packagePatterns = packagePatterns,
                allowedPackages = allowedPackages,
                maxSubdomainDepth = maxSubdomainDepth,
                maxEntropy = maxEntropy,
                expression = expression
            ),
            action = containmentAction
        )

        EDRLogger.d(TAG, "Successfully constructed v2.0 YAML rule: ${rule.canonicalId} (${rule.name}) [Engine: $engineType]")
        return rule
    }
}
