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
    val name: String,
    val description: String,
    val severity: String,
    val category: String,
    val condition: RuleCondition,
    val action: ContainmentAction
)

@Serializable
data class RuleCondition(
    @SerialName("target_ips") val targetIps: List<String> = emptyList(),
    @SerialName("target_domains") val targetDomains: List<String> = emptyList(),
    @SerialName("target_ports") val targetPorts: List<Int> = emptyList(),
    @SerialName("package_patterns") val packagePatterns: List<String> = emptyList(),
    @SerialName("allowed_packages") val allowedPackages: List<String> = emptyList()
)

@Serializable
enum class ContainmentAction {
    @SerialName("LOG") LOG,
    @SerialName("NOTIFY") NOTIFY,
    @SerialName("BLOCK") BLOCK,
    @SerialName("TERMINATE") TERMINATE
}
