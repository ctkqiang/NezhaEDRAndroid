package xin.ctkqiang.nezhaedr.containment

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.util.UUID

data class SIEMThreatLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val ruleId: String,
    val ruleName: String,
    val severity: String,
    val actionTaken: String,
    val appName: String = "System / Network Stack",
    val packageName: String = "",
    val details: String
)

class ContainmentManager(
    private val context: Context,
    private val notificationManager: EDRNotificationManager = EDRNotificationManager(context)
) {

    private val _siemLogs = MutableSharedFlow<SIEMThreatLog>(extraBufferCapacity = 100)
    val siemLogs: SharedFlow<SIEMThreatLog> = _siemLogs.asSharedFlow()

    fun handleRuleMatch(matchResult: RuleMatchResult) {
        val rule = matchResult.rule
        val event = matchResult.event
        EDRLogger.w(TAG, "handleRuleMatch: Processing match for Rule ${rule.id} (${rule.name}) [Severity: ${rule.severity}, Action: ${rule.action}]")

        val (appName, pkgName, eventDetails) = when (event) {
            is TelemetryEvent.Network -> {
                EDRLogger.d(TAG, "handleRuleMatch: Classifying Network match for '${event.destinationIp}'")
                Triple(
                    "Network Egress Socket",
                    event.destinationIp,
                    "Destination: ${event.destinationIp}:${event.destinationPort} (${event.protocol}) — ${rule.description}"
                )
            }
            is TelemetryEvent.Package -> {
                val appLabel = getAppNameFromPackage(event.packageName)
                EDRLogger.d(TAG, "handleRuleMatch: Classifying Package match for '${event.packageName}' (App: '$appLabel')")
                Triple(
                    appLabel,
                    event.packageName,
                    "Package: ${event.packageName} (${event.action}) — ${rule.description}"
                )
            }
        }

        val logEntry = SIEMThreatLog(
            ruleId = rule.id,
            ruleName = rule.name,
            severity = rule.severity,
            actionTaken = rule.action.name,
            appName = appName,
            packageName = pkgName,
            details = eventDetails
        )
        val emitted = _siemLogs.tryEmit(logEntry)
        EDRLogger.d(TAG, "handleRuleMatch: Logged to SIEM flow (emitted: $emitted)")

        when (rule.action) {
            ContainmentAction.LOG -> {
                EDRLogger.i(TAG, "Containment Action LOG executed for Rule ${rule.id}")
            }
            ContainmentAction.NOTIFY -> {
                EDRLogger.i(TAG, "Containment Action NOTIFY executed for Rule ${rule.id}")
                notificationManager.sendThreatNotification(matchResult)
            }
            ContainmentAction.BLOCK, ContainmentAction.TERMINATE -> {
                EDRLogger.w(TAG, "Containment Action ${rule.action.name} executed for Rule ${rule.id}")
                notificationManager.sendThreatNotification(matchResult)
            }
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            EDRLogger.v(TAG, "getAppNameFromPackage: App name lookup fallback for '$packageName'")
            packageName
        }
    }

    companion object {
        private const val TAG = "ContainmentManager"
    }
}
