package xin.ctkqiang.nezhaedr.containment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import xin.ctkqiang.nezhaedr.R
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.util.EDRLogger

class EDRNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        EDRLogger.w(TAG, "sendThreatNotification: Preparing threat alert for Rule ${rule.id} (${rule.name})")

        val title = context.getString(R.string.threat_detected_title, rule.name)
        val body = context.getString(
            R.string.threat_detected_body,
            rule.severity,
            rule.description
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(rule.id.hashCode(), notification)
            EDRLogger.i(TAG, "Threat notification posted successfully for Rule ${rule.id}")
        } catch (e: Exception) {
            EDRLogger.e(TAG, "Failed to post notification for Rule ${rule.id}", e)
        }
    }

    companion object {
        private const val TAG = "EDRNotificationManager"
        const val CHANNEL_ID = "nezha_edr_threat_channel"
    }
}
