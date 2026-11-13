package xin.ctkqiang.nezhaedr.containment

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent
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

        val (appName, pkgName, eventDetails) = when (event) {
            is TelemetryEvent.Network -> {
                Triple(
                    "Network Egress Socket",
                    event.destinationIp,
                    "Destination: ${event.destinationIp}:${event.destinationPort} (${event.protocol}) — ${rule.description}"
                )
            }
            is TelemetryEvent.Package -> {
                val appLabel = getAppNameFromPackage(event.packageName)
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
        _siemLogs.tryEmit(logEntry)

        when (rule.action) {
            ContainmentAction.LOG -> {
            }
            ContainmentAction.NOTIFY -> {
                notificationManager.sendThreatNotification(matchResult)
            }
            ContainmentAction.BLOCK, ContainmentAction.TERMINATE -> {
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
            packageName
        }
    }
}
