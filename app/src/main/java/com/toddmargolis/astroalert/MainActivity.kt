package com.toddmargolis.astroalert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.toddmargolis.astroalert.data.AppDatabase
import com.toddmargolis.astroalert.data.AstroRepository
import com.toddmargolis.astroalert.data.MockDataProvider
import com.toddmargolis.astroalert.data.api.SunTimes
import com.toddmargolis.astroalert.data.models.ConditionCheckResult
import com.toddmargolis.astroalert.data.models.ObservingLocation
import com.toddmargolis.astroalert.data.models.TelescopiusTarget
import com.toddmargolis.astroalert.domain.ConditionEvaluator
import com.toddmargolis.astroalert.domain.SunTimeHelper
import com.toddmargolis.astroalert.domain.ObservingConditions
import com.toddmargolis.astroalert.workers.WorkScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var textView: TextView
    private lateinit var checkButton: Button
    private lateinit var showResultsButton: Button
    private val repository = AstroRepository()
    private val evaluator = ConditionEvaluator()

    // Mock mode toggle - set to true to use mock data
    private val MOCK_MODE = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)
        textView = findViewById(R.id.textView)
        checkButton = findViewById(R.id.checkNowButton)
        showResultsButton = findViewById(R.id.showResultsButton)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        // Schedule the background checks
        WorkScheduler.scheduleWeatherChecks(this)

        // Set up button click listeners
        checkButton.setOnClickListener {
            checkButton.isEnabled = false
            checkButton.text = "Checking..."
            lifecycleScope.launch {
                try {
                    performManualCheck()
                } catch (e: Exception) {
                    updateStatus("Error: ${e.message}\n\n${e.stackTraceToString()}")
                } finally {
                    checkButton.isEnabled = true
                    checkButton.text = "Check Now"
                }
            }
        }

        showResultsButton.setOnClickListener {
            lifecycleScope.launch {
                displayLastChecks()
            }
        }

        // Display last check results
        lifecycleScope.launch {
            displayLastChecks()
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            textView.text = message
        }
    }

    private suspend fun performManualCheck() {
        val statusLog = StringBuilder()
        statusLog.append("CHECKING CONDITIONS")
        if (MOCK_MODE) statusLog.append(" [MOCK MODE]")
        statusLog.append("\n")
        statusLog.append("=".repeat(40) + "\n\n")
        updateStatus(statusLog.toString())

        val currentTime = System.currentTimeMillis()

        // PHASE 1: Check all locations and find the best one
        val locationScores = mutableListOf<Triple<ObservingLocation, ObservingConditions, Double>>()

        for (location in ObservingLocation.ENABLED_LOCATIONS) {
            try {
                statusLog.append("📍 ${location.name}\n")
                updateStatus(statusLog.toString())
                kotlinx.coroutines.delay(200)

                // Get sunrise/sunset, forecast, and sky data (your existing code)
                val sunTimesResult = repository.getSunTimes(location.latitude, location.longitude)
                val (sunsetHour, sunriseHour) = if (sunTimesResult.isSuccess) {
                    val sunTimes = sunTimesResult.getOrNull() as com.toddmargolis.astroalert.data.api.SunTimes
                    val sunset = SunTimeHelper.parseToLocalHour(sunTimes.sunset)
                    val sunrise = SunTimeHelper.parseToLocalHour(sunTimes.sunrise)
                    statusLog.append("  ✓ Sunset: ${sunset}:00, Sunrise: ${sunrise}:00\n")
                    Pair(sunset, sunrise)
                } else {
                    statusLog.append("  ⚠️ Using default times (sunset 17:00, sunrise 07:00)\n")
                    Pair(17, 7)
                }
                updateStatus(statusLog.toString())

                val forecastResult = if (MOCK_MODE) {
                    Result.success(MockDataProvider.getMockForecast())
                } else {
                    repository.getWeatherForecast(location.latitude, location.longitude)
                }

                if (forecastResult.isFailure) {
                    val error = forecastResult.exceptionOrNull()
                    statusLog.append("  ❌ Forecast failed: ${error?.message}\n\n")
                    updateStatus(statusLog.toString())
                    continue
                }

                val forecast = forecastResult.getOrNull()
                if (forecast == null) {
                    statusLog.append("  ⚠️ Forecast data unavailable\n\n")
                    updateStatus(statusLog.toString())
                    continue
                }
                val skyResult = if (MOCK_MODE) {
                    Result.success(MockDataProvider.getMockSkyData())
                } else {
                    repository.getSkyData(location.latitude, location.longitude, currentTime)
                }
                val skyData = skyResult.getOrNull()

                // Evaluate conditions
                val conditions = evaluator.evaluateNightConditions(
                    forecast,
                    skyData,
                    sunsetHour,
                    sunriseHour
                )

                // Calculate location score
                val score = calculateLocationScore(conditions)
                locationScores.add(Triple(location, conditions, score))

                statusLog.append("  Score: ${score.toInt()}\n")
                statusLog.append("  ${if (conditions.isGood) "✅" else "❌"} ${conditions.message}\n\n")
                updateStatus(statusLog.toString())

            } catch (e: Exception) {
                statusLog.append("  ❌ Exception: ${e.message}\n\n")
                updateStatus(statusLog.toString())
                e.printStackTrace()
            }
        }

        // PHASE 2: Pick the best location and calculate targets for it
        if (locationScores.isNotEmpty()) {
            val bestLocationData = locationScores.maxByOrNull { it.third }!!
            val bestLocation = bestLocationData.first
            val bestConditions = bestLocationData.second

            statusLog.append("=".repeat(40) + "\n")
            statusLog.append("🏆 BEST LOCATION: ${bestLocation.name}\n")
            statusLog.append("=".repeat(40) + "\n\n")
            updateStatus(statusLog.toString())

            // Now get targets for THIS specific location
            calculateTargetsForLocation(bestLocation, bestConditions, statusLog)

            // Save to database
            val checkResult = ConditionCheckResult(
                locationName = bestLocation.name,
                checkTime = currentTime,
                isGood = bestConditions.isGood,
                cloudCover = bestConditions.cloudCover,
                seeing = bestConditions.seeing,
                transparency = bestConditions.transparency,
                moonIllumination = null,
                moonAltitude = null,
                message = bestConditions.message,
                recommendedTargets = null // Will add this later
            )
            database.conditionDao().insertCheck(checkResult)
        }

        statusLog.append("=".repeat(40) + "\n")
        statusLog.append("Check complete!\n\n")
        statusLog.append("Tap 'Show Results' to see summary\n")
        updateStatus(statusLog.toString())
    }

    private fun calculateHourAngle(ra: Double, longitude: Double, time: Calendar): Double {
        // This one already accepts longitude - it's correct!
        val utcTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val j2000 = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        j2000.set(2000, 0, 1, 12, 0, 0)
        j2000.set(Calendar.MILLISECOND, 0)

        val daysSinceJ2000 = (time.timeInMillis - j2000.timeInMillis) / (1000.0 * 86400.0)
        val gmst0 = 100.46 + 0.985647 * daysSinceJ2000

        val utHours = time.get(Calendar.HOUR_OF_DAY) +
                time.get(Calendar.MINUTE) / 60.0 +
                time.get(Calendar.SECOND) / 3600.0

        val gmst = (gmst0 + utHours * 15.0) % 360
        val gmstHours = gmst / 15.0
        val lst = (gmstHours + longitude / 15.0 + 24) % 24

        // Hour angle = LST - RA
        return (lst - ra + 24) % 24
    }

    private fun calculateAltitudeAtTime(dec: Double, latitude: Double, hourAngle: Double): Double {
        // This one already accepts latitude - it's correct!
        val decRad = Math.toRadians(dec)
        val latRad = Math.toRadians(latitude)
        val haRad = Math.toRadians(hourAngle * 15.0)

        val sinAlt = Math.sin(decRad) * Math.sin(latRad) +
                Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad)

        return Math.toDegrees(Math.asin(sinAlt))
    }

    private suspend fun displayLastChecks() {
        val results = StringBuilder()
        results.append("LAST CHECK RESULTS\n")
        results.append("=".repeat(40) + "\n\n")

        var hasData = false

        for (location in ObservingLocation.ENABLED_LOCATIONS) {
            val lastCheck = database.conditionDao().getLatestCheckForLocation(location.name)

            if (lastCheck != null) {
                hasData = true
                val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                val timeStr = dateFormat.format(Date(lastCheck.checkTime))

                val status = if (lastCheck.isGood) "✅ GOOD" else "❌ POOR"

                results.append("📍 ${location.name}\n")
                results.append("$timeStr\n")
                results.append("$status\n")
                results.append("${lastCheck.message}\n\n")
                results.append("Cloud: ${lastCheck.cloudCover.toInt()}% | ")
                results.append("Seeing: ${lastCheck.seeing.toInt()}/5 | ")
                results.append("Trans: ${lastCheck.transparency.toInt()}\n")

                if (lastCheck.moonIllumination != null) {
                    results.append("Moon: ${lastCheck.moonIllumination.toInt()}% ")
                    if (lastCheck.moonAltitude != null) {
                        results.append(if (lastCheck.moonAltitude > 0) "above horizon" else "below horizon")
                    }
                    results.append("\n")
                }

                // Show targets if available
                if (lastCheck.recommendedTargets != null) {
                    try {
                        val targetsArray = org.json.JSONArray(lastCheck.recommendedTargets)
                        if (targetsArray.length() > 0) {
                            results.append("\n🎯 Targets:\n")
                            for (i in 0 until minOf(3, targetsArray.length())) {
                                val target = targetsArray.getJSONObject(i)
                                results.append("• ${target.getString("name")}\n")
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore JSON parsing errors
                    }
                }

                results.append("\n")
            }
        }

        if (!hasData) {
            results.append("No checks yet.\n\n")
            results.append("Tap 'Check Now' to get current\n")
            results.append("conditions, or wait for automatic\n")
            results.append("checks at 6 AM, 11 AM, and 4 PM.\n\n")
        }

        results.append("=".repeat(40) + "\n")
        results.append("Monitoring: Indiana Dunes, Hebron,\n")
        results.append("Zion, Afton Forest Preserve\n")

        runOnUiThread {
            textView.text = results.toString()
        }
    }

    private fun getSeasonListIds(month: Int): List<Pair<String, String>> {
        // month is 0-11 (0=Jan, 11=Dec)
        // Returns list of (ID, Name) pairs
        return when (month) {
            0, 1 -> listOf(Pair("4ac5209f", "Winter")) // Jan, Feb
            2 -> listOf(Pair("4ac5209f", "Winter"), Pair("a77bb683", "Spring")) // Mar - transition
            3, 4 -> listOf(Pair("a77bb683", "Spring")) // Apr, May
            5 -> listOf(Pair("a77bb683", "Spring"), Pair("be1f7d01", "Summer")) // Jun - transition
            6, 7 -> listOf(Pair("be1f7d01", "Summer")) // Jul, Aug
            8 -> listOf(Pair("be1f7d01", "Summer"), Pair("9cf4f90c", "Fall")) // Sep - transition
            9, 10 -> listOf(Pair("9cf4f90c", "Fall")) // Oct, Nov
            11 -> listOf(Pair("9cf4f90c", "Fall"), Pair("4ac5209f", "Winter")) // Dec - transition
            else -> listOf(Pair("9cf4f90c", "Fall"))
        }
    }

    private fun calculateTransitTime(ra: Double, longitude: Double, currentTime: Calendar): Calendar {
        // RA is in hours (0-24)
        // Transit occurs when Local Sidereal Time (LST) = RA
        // longitude parameter is used directly (negative = west)

        // Get current date/time in UTC
        val utcTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))

        // Calculate days since J2000.0 (Jan 1, 2000, 12:00 UT)
        val j2000 = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        j2000.set(2000, 0, 1, 12, 0, 0)
        j2000.set(Calendar.MILLISECOND, 0)

        val daysSinceJ2000 = (utcTime.timeInMillis - j2000.timeInMillis) / (1000.0 * 86400.0)

        // Calculate Greenwich Mean Sidereal Time (GMST) at 0h UT
        val gmst0 = 100.46 + 0.985647 * daysSinceJ2000

        // Current hour angle
        val utHours = utcTime.get(Calendar.HOUR_OF_DAY) +
                utcTime.get(Calendar.MINUTE) / 60.0 +
                utcTime.get(Calendar.SECOND) / 3600.0

        // Current GMST
        val gmst = (gmst0 + utHours * 15.0) % 360

        // Convert to hours
        val gmstHours = gmst / 15.0

        // Local Sidereal Time = GMST + longitude (in hours)
        val lst = (gmstHours + longitude / 15.0 + 24) % 24

        // Calculate hours until transit
        var hoursUntilTransit = (ra - lst + 24) % 24

        // If transit is very soon (< 1 hour) or already passed, might want next day's transit
        if (hoursUntilTransit < 1) {
            hoursUntilTransit += 24
        }

        // Calculate transit time in local time
        val transitTime = currentTime.clone() as Calendar
        transitTime.add(Calendar.MINUTE, (hoursUntilTransit * 60).toInt())

        return transitTime
    }

    private fun calculateAltitude(dec: Double, latitude: Double): Double {
        // Maximum altitude at transit (crossing meridian)
        // For northern hemisphere observer at latitude φ:
        // If dec > φ: object passes through zenith, alt = 90°
        // If dec < φ: alt = 90° - (φ - dec)

        // Maximum altitude when on meridian
        return 90.0 - Math.abs(latitude - dec)
    }

    private fun formatTime(time: Calendar): String {
        val hourFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        val now = Calendar.getInstance()
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)

        return when {
            time.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> {
                "Tonight ${hourFormat.format(time.time)}"
            }
            time.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> {
                "Tomorrow ${hourFormat.format(time.time)}"
            }
            else -> {
                "${dateFormat.format(time.time)} ${hourFormat.format(time.time)}"
            }
        }
    }

    private fun calculateLocationScore(conditions: ObservingConditions): Double {
        // Start with a perfect score
        var score = 100.0

        // Penalize heavily for clouds (0% = -0 points, 100% = -100 points)
        score -= conditions.cloudCover

        // Add bonus for good seeing (1-5 scale)
        score += (conditions.seeing * 5.0)

        // Add bonus for transparency (assuming 0-30 scale)
        score += (conditions.transparency / 2.0)

        return score
    }

    private suspend fun calculateTargetsForLocation(
        location: ObservingLocation,
        conditions: ObservingConditions,
        statusLog: StringBuilder
    ) {
        if (!conditions.isGood && !MOCK_MODE) {
            statusLog.append("Conditions not good enough for imaging\n")
            updateStatus(statusLog.toString())
            return
        }

        // Get sunset/sunrise for this location
        val sunTimesResult = repository.getSunTimes(location.latitude, location.longitude)
        val (sunsetHour, sunriseHour) = if (sunTimesResult.isSuccess) {
            val sunTimes = sunTimesResult.getOrNull() as com.toddmargolis.astroalert.data.api.SunTimes
            Pair(SunTimeHelper.parseToLocalHour(sunTimes.sunset), SunTimeHelper.parseToLocalHour(sunTimes.sunrise))
        } else {
            Pair(17, 7)
        }

        statusLog.append("  Fetching seasonal targets...\n")
        updateStatus(statusLog.toString())

        // Get seasonal targets
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val seasonIds = getSeasonListIds(month)
        val allTargets = mutableListOf<TelescopiusTarget>()

        for ((listId, seasonName) in seasonIds) {
            val listResult = repository.getTargetList(listId)
            listResult.fold(
                onSuccess = { list ->
                    if (list.objects != null && list.objects.isNotEmpty()) {
                        val targets = list.objects.map { obj ->
                            TelescopiusTarget(
                                id = listId,
                                name = obj.name,
                                type = null,
                                rightAscension = obj.coordinates?.ra,
                                declination = obj.coordinates?.dec,
                                magnitude = null,
                                constellation = null
                            )
                        }
                        allTargets.addAll(targets)
                    }
                },
                onFailure = { /* ignore */ }
            )
        }

        // Calculate quality scores using THIS location's coordinates
        if (allTargets.isNotEmpty()) {
            val currentTime = Calendar.getInstance()

            val sessionStart = currentTime.clone() as Calendar
            sessionStart.set(Calendar.HOUR_OF_DAY, sunsetHour)
            sessionStart.set(Calendar.MINUTE, 0)

            val sessionEnd = currentTime.clone() as Calendar
            sessionEnd.set(Calendar.HOUR_OF_DAY, sunriseHour)
            sessionEnd.set(Calendar.MINUTE, 0)
            if (sessionEnd.before(sessionStart)) {
                sessionEnd.add(Calendar.DAY_OF_MONTH, 1)
            }

            val effectiveStart = if (currentTime.after(sessionStart)) currentTime.clone() as Calendar else sessionStart

            val scoredTargets = allTargets
                .mapNotNull { target ->
                    if (target.rightAscension == null || target.declination == null) return@mapNotNull null

                    val transitTime = calculateTransitTime(target.rightAscension, location.longitude, currentTime)
                    val maxAltitude = calculateAltitude(target.declination, location.latitude)

                    var qualityHours = 0.0
                    val testTime = effectiveStart.clone() as Calendar

                    while (testTime.before(sessionEnd)) {
                        // Use THIS location's coordinates
                        val hourAngle = calculateHourAngle(target.rightAscension, location.longitude, testTime)
                        val altitude = calculateAltitudeAtTime(target.declination, location.latitude, hourAngle)

                        if (altitude < 25.0) {
                            testTime.add(Calendar.HOUR, 1)
                            continue
                        }

                        when {
                            altitude >= 60 -> qualityHours += 1.0
                            altitude >= 45 -> qualityHours += 0.8
                            altitude >= 30 -> qualityHours += 0.5
                            else -> qualityHours += 0.1
                        }

                        testTime.add(Calendar.HOUR, 1)
                    }

                    if (qualityHours >= 1.0) {
                        Pair(Triple(target, transitTime, maxAltitude), qualityHours)
                    } else {
                        null
                    }
                }
                .sortedByDescending { it.second }
                .take(5)

            if (scoredTargets.isNotEmpty()) {
                statusLog.append("\n  🎯 RECOMMENDED TARGETS:\n")
                scoredTargets.forEach { (triple, qualityHours) ->
                    val (target, transitTime, maxAltitude) = triple
                    statusLog.append("  • ${target.name}\n")
                    statusLog.append("    Peak: ${formatTime(transitTime)} (${maxAltitude.toInt()}°)\n")
                    statusLog.append("    Quality time: ${qualityHours.toInt()}h\n")
                }
                statusLog.append("\n")
            }
        }

        updateStatus(statusLog.toString())
    }
}
