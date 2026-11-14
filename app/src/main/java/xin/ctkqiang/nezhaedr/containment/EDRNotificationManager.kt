package xin.ctkqiang.nezhaedr.containment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import xin.ctkqiang.nezhaedr.R
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.util.concurrent.ConcurrentHashMap

class EDRNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val lastNotificationTimestamps = ConcurrentHashMap<String, Long>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            EDRLogger.d(TAG, "createNotificationChannel: Creating NotificationChannel '$CHANNEL_ID'")
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
            EDRLogger.i(TAG, "Notification channel '$CHANNEL_ID' created successfully")
        }
    }

    fun sendThreatNotification(matchResult: RuleMatchResult) {
        val rule = matchResult.rule
        val now = System.currentTimeMillis()
        val cooldownKey = rule.canonicalId

        val lastSent = lastNotificationTimestamps[cooldownKey] ?: 0L
        if (now - lastSent < NOTIFICATION_COOLDOWN_MS) {
            EDRLogger.d(TAG, "sendThreatNotification: Throttling duplicate notification for Rule ${rule.canonicalId} (within ${NOTIFICATION_COOLDOWN_MS / 1000}s cooldown)")
            return
        }

        lastNotificationTimestamps[cooldownKey] = now
        EDRLogger.w(TAG, "sendThreatNotification: Preparing single threat alert for Rule ${rule.canonicalId} (${rule.name})")

        val appName = matchResult.event.let {
            if (it is xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent.Package) it.packageName else "Browser Egress"
        }
        val title = context.getString(R.string.threat_detected_title, rule.name)
        val body = context.getString(
            R.string.threat_detected_body,
            appName,
            rule.description
        )

        val customLayout = RemoteViews(context.packageName, R.layout.notification_threat_alert).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
            setTextViewText(R.id.notif_details, "Rule: ${rule.canonicalId} (${rule.name})")
            setTextViewText(R.id.notif_badge, rule.severity)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_power)
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        notification.flags = notification.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR

        try {
            notificationManager.notify(rule.canonicalId.hashCode(), notification)
            EDRLogger.i(TAG, "Custom RemoteViews threat notification posted successfully for Rule ${rule.canonicalId}")
        } catch (e: Exception) {
            EDRLogger.e(TAG, "Failed to post custom notification for Rule ${rule.canonicalId}", e)
        }
    }

    companion object {
        private const val TAG = "EDRNotificationManager"
        const val CHANNEL_ID = "nezha_edr_threat_channel"
        private const val NOTIFICATION_COOLDOWN_MS = 30_000L // 30 seconds notification debouncing window
    }
}
