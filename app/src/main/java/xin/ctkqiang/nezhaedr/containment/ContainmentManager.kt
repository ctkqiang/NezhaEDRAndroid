package xin.ctkqiang.nezhaedr.containment

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.util.UUID

data class ThreatLogEntry(
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

    private val _threatLogs = MutableSharedFlow<ThreatLogEntry>(extraBufferCapacity = 100)
    val threatLogs: SharedFlow<ThreatLogEntry> = _threatLogs.asSharedFlow()

    fun handleRuleMatch(matchResult: RuleMatchResult) {
        val rule = matchResult.rule
        val event = matchResult.event
        EDRLogger.w(TAG, "handleRuleMatch: Processing match for Rule ${rule.id} (${rule.name}) [Severity: ${rule.severity}, Action: ${rule.action}]")

        val (appName, pkgName, eventDetails) = when (event) {
            is TelemetryEvent.Network -> {
                EDRLogger.d(TAG, "handleRuleMatch: Classifying Network match for '${event.destinationIp}'")
                Triple(
                    "Network Protection Sentinel",
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

        val logEntry = ThreatLogEntry(
            ruleId = rule.id,
            ruleName = rule.name,
            severity = rule.severity,
            actionTaken = rule.action.name,
            appName = appName,
            packageName = pkgName,
            details = eventDetails
        )
        val emitted = _threatLogs.tryEmit(logEntry)
        EDRLogger.d(TAG, "handleRuleMatch: Logged to threat log flow (emitted: $emitted)")

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

                if (event is TelemetryEvent.Package || rule.action == ContainmentAction.TERMINATE) {
                    terminateProcessOrReturnHome(pkgName)
                }
            }
        }
    }

    private fun terminateProcessOrReturnHome(packageName: String) {
        try {
            if (packageName.isNotEmpty()) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.killBackgroundProcesses(packageName)
                EDRLogger.w(TAG, "Executed killBackgroundProcesses for rogue package: $packageName")
            }

            // Launch Home Intent to return device to launcher if active
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            EDRLogger.w(TAG, "Triggered HOME intent containment action for terminated app")
        } catch (e: Exception) {
            EDRLogger.e(TAG, "Failed to execute process containment for $packageName", e)
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
