package xin.ctkqiang.nezhaedr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetectionRulePackage(
    val version: String,
    @SerialName("updated_at") val updatedAt: String,
    val rules: List<DetectionRule>
)

@Serializable
data class DetectionRule(
    val id: String,
    @SerialName("schema_version") val schemaVersion: String = "2.0",
    @SerialName("rule_id") val ruleId: String = "",
    val name: String,
    val description: String,
    val severity: String,
    val category: String,
    val enabled: Boolean = true,
    @SerialName("engine_type") val engineType: String = "signature",
    @SerialName("target_source") val targetSource: String = "dns_query",
    val condition: RuleCondition,
    val action: ContainmentAction
) {
    // Helper property to get canonical rule identifier
    val canonicalId: String
        get() = ruleId.ifEmpty { id }
}

@Serializable
data class RuleCondition(
    @SerialName("target_ips") val targetIps: List<String> = emptyList(),
    @SerialName("target_domains") val targetDomains: List<String> = emptyList(),
    @SerialName("target_ports") val targetPorts: List<Int> = emptyList(),
    @SerialName("package_patterns") val packagePatterns: List<String> = emptyList(),
    @SerialName("allowed_packages") val allowedPackages: List<String> = emptyList(),
    @SerialName("max_subdomain_depth") val maxSubdomainDepth: Int? = null,
    @SerialName("max_entropy") val maxEntropy: Double? = null,
    val expression: String? = null
)

@Serializable
enum class ContainmentAction {
    @SerialName("LOG") LOG,
    @SerialName("NOTIFY") NOTIFY,
    @SerialName("BLOCK") BLOCK,
    @SerialName("TERMINATE") TERMINATE
}
