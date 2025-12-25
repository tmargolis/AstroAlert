package com.toddmargolis.astroalert.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun scheduleWeatherChecks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Schedule checks at 6 AM, 11 AM, and 4 PM
        scheduleCheckAt(context, 6, "morning_check")
        scheduleCheckAt(context, 11, "midday_check")
        scheduleCheckAt(context, 16, "afternoon_check")
    }

    private fun scheduleCheckAt(context: Context, hour: Int, workName: String) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // If target time has passed today, schedule for tomorrow
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        // Create a periodic work request that runs daily
        val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}