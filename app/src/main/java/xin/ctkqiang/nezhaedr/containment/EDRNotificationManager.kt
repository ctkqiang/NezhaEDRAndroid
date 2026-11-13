package xin.ctkqiang.nezhaedr.containment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import xin.ctkqiang.nezhaedr.R
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult

class EDRNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendThreatNotification(matchResult: RuleMatchResult) {
        val title = context.getString(R.string.threat_detected_title, matchResult.rule.name)
        val body = context.getString(
            R.string.threat_detected_body,
            matchResult.rule.severity,
            matchResult.rule.description
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(matchResult.rule.id.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "nezha_edr_threat_channel"
    }
}
