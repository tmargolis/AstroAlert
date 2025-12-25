package com.toddmargolis.astroalert.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.toddmargolis.astroalert.data.AppDatabase
import com.toddmargolis.astroalert.data.AstroRepository
import com.toddmargolis.astroalert.data.models.ConditionCheckResult
import com.toddmargolis.astroalert.data.models.ObservingLocation
import com.toddmargolis.astroalert.domain.ConditionEvaluator
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

class WeatherCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = AstroRepository()
    private val evaluator = ConditionEvaluator()
    private val database = AppDatabase.getDatabase(context)

    override suspend fun doWork(): Result {
        return try {
            checkConditionsAndNotify()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun checkConditionsAndNotify() = coroutineScope {
        val goodLocations = mutableListOf<String>()
        val currentTime = System.currentTimeMillis()

        // Approximate sunset/sunrise hours (we'll use sky API for exact times)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val sunsetHour = 18 // Approximate - winter sunset around 4-5pm, we check from current hour
        val sunriseHour = 6 // Approximate - winter sunrise around 7am

        for (location in ObservingLocation.ENABLED_LOCATIONS) {
            // Get forecast
            val forecastResult = repository.getWeatherForecast(
                location.latitude,
                location.longitude
            )

            // Get sky data for moon/sun
            val skyResult = repository.getSkyData(
                location.latitude,
                location.longitude,
                currentTime
            )

            forecastResult.fold(
                onSuccess = { forecast ->
                    val skyData = skyResult.getOrNull()

                    val sunsetHour = 17  // ~5 PM sunset
                    val sunriseHour = 7  // ~7 AM sunrise

                    val conditions = evaluator.evaluateNightConditions(
                        forecast,
                        skyData,
                        sunsetHour,
                        sunriseHour
                    )

                    // Save to database
                    val checkResult = ConditionCheckResult(
                        locationName = location.name,
                        checkTime = currentTime,
                        isGood = conditions.isGood,
                        cloudCover = conditions.cloudCover,
                        seeing = conditions.seeing,
                        transparency = conditions.transparency,
                        moonIllumination = skyData?.moon?.illumination,
                        moonAltitude = skyData?.moon?.altitude,
                        message = conditions.message
                    )
                    database.conditionDao().insertCheck(checkResult)

                    if (conditions.isGood) {
                        goodLocations.add(location.name)
                    }
                },
                onFailure = { error ->
                    error.printStackTrace()
                }
            )
        }

        // Clean up old checks (older than 7 days)
        val sevenDaysAgo = currentTime - (7 * 24 * 60 * 60 * 1000)
        database.conditionDao().deleteOldChecks(sevenDaysAgo)

        // Send notification if any locations have good conditions
        if (goodLocations.isNotEmpty()) {
            sendNotification(goodLocations)
        }
    }

    private fun sendNotification(locations: List<String>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        val channelId = "astro_alert_channel"
        val channel = NotificationChannel(
            channelId,
            "Astrophotography Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for good astrophotography conditions"
        }
        notificationManager.createNotificationChannel(channel)

        // Build notification
        val locationText = locations.joinToString(", ")
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Clear Skies Tonight! 🌟")
            .setContentText("Good conditions at: $locationText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Perfect astrophotography conditions detected at:\n$locationText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}