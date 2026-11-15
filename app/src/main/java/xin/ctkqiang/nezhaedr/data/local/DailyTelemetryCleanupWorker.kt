package xin.ctkqiang.nezhaedr.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyTelemetryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        EDRLogger.w(TAG, "doWork: Executing 12:00 AM midnight cron job to clear local SQLite telemetry history")
        return try {
            val dbHelper = TelemetryDatabaseHelper(applicationContext)
            dbHelper.clearAllVisitedSites()
            EDRLogger.i(TAG, "doWork: 12:00 AM midnight cron job completed successfully. SQLite history purged.")
            Result.success()
        } catch (e: Exception) {
            EDRLogger.e(TAG, "doWork: Failed to execute 12:00 AM midnight cron job", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DailyTelemetryCleanupWorker"
        const val WORK_NAME = "DailyTelemetryCleanupMidnightCron"

        fun scheduleMidnightCron(context: Context) {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val initialDelayMs = midnight.timeInMillis - now.timeInMillis
            EDRLogger.i(TAG, "scheduleMidnightCron: Scheduling daily 12:00 AM cron with initial delay of ${initialDelayMs / 1000 / 60} minutes")

            val workRequest = PeriodicWorkRequestBuilder<DailyTelemetryCleanupWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
