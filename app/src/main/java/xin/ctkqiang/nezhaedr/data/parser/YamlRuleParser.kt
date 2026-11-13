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
        var name = ""
        var description = ""
        var severity = "MEDIUM"
        var category = "NETWORK"
        var actionStr = "LOG"

        val targetIps = mutableListOf<String>()
        val targetDomains = mutableListOf<String>()
        val targetPorts = mutableListOf<Int>()
        val packagePatterns = mutableListOf<String>()
        val allowedPackages = mutableListOf<String>()

        var currentSection = ""

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.startsWith("id:")) {
                id = trimmed.substringAfter("id:").trim().removeSurrounding("\"")
                EDRLogger.v(TAG, "Parsed rule ID: $id")
            } else if (trimmed.startsWith("name:")) {
                name = trimmed.substringAfter("name:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("description:")) {
                description = trimmed.substringAfter("description:").trim().removeSurrounding("\"")
            } else if (trimmed.startsWith("severity:")) {
                severity = trimmed.substringAfter("severity:").trim().removeSurrounding("\"").uppercase()
            } else if (trimmed.startsWith("category:")) {
                category = trimmed.substringAfter("category:").trim().removeSurrounding("\"").uppercase()
            } else if (trimmed.startsWith("action:")) {
                actionStr = trimmed.substringAfter("action:").trim().removeSurrounding("\"").uppercase()
            } else if (trimmed == "condition:") {
                currentSection = "condition"
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

        if (id.isEmpty() || name.isEmpty()) {
            EDRLogger.w(TAG, "Failed to parse YAML rule: missing required id or name field")
            return null
        }

        val containmentAction = try {
            ContainmentAction.valueOf(actionStr)
        } catch (e: Exception) {
            EDRLogger.w(TAG, "Invalid action '$actionStr' for rule $id, defaulting to LOG", e)
            ContainmentAction.LOG
        }

        val rule = DetectionRule(
            id = id,
            name = name,
            description = description,
            severity = severity,
            category = category,
            condition = RuleCondition(
                targetIps = targetIps,
                targetDomains = targetDomains,
                targetPorts = targetPorts,
                packagePatterns = packagePatterns,
                allowedPackages = allowedPackages
            ),
            action = containmentAction
        )

        EDRLogger.d(TAG, "Successfully constructed YAML rule: ${rule.id} (${rule.name})")
        return rule
    }
}
