package com.toddmargolis.astroalert.domain

import com.toddmargolis.astroalert.BuildConfig
import com.toddmargolis.astroalert.data.models.AstrosphericForecast
import com.toddmargolis.astroalert.data.models.SkyData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class ObservingConditions(
    val isGood: Boolean,
    val cloudCover: Double,
    val seeing: Double,
    val transparency: Double,
    val moonIllumination: Double?,
    val moonAltitude: Double?,
    val message: String
)

class ConditionEvaluator {

    suspend fun evaluateNightConditions(
        forecast: AstrosphericForecast,
        skyData: SkyData?,
        sunsetHour: Int,
        sunriseHour: Int
    ): ObservingConditions {
        // Gather data for the entire night
        val nightData = buildNightDataSummary(forecast, skyData, sunsetHour, sunriseHour)

        // Use Gemini API to analyze conditions
        val analysis = analyzeWithGemini(nightData)

        return analysis
    }

    private fun buildNightDataSummary(
        forecast: AstrosphericForecast,
        skyData: SkyData?,
        sunsetHour: Int,
        sunriseHour: Int
    ): String {
        val summary = StringBuilder()

        summary.append("Analyze tonight's astrophotography conditions from sunset until 2 hours before sunrise:\n\n")

        // Calculate current hour to determine offset to sunset
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Calculate hours from now until sunset
        val hoursToSunset = if (sunsetHour >= currentHour) {
            sunsetHour - currentHour
        } else {
            // Sunset is tomorrow (past midnight)
            24 - currentHour + sunsetHour
        }

        // Calculate hours from now until 2 hours before sunrise
        val adjustedSunriseHour = sunriseHour - 2
        val hoursToEndOfNight = if (adjustedSunriseHour >= currentHour) {
            adjustedSunriseHour - currentHour
        } else {
            // Sunrise is tomorrow
            24 - currentHour + adjustedSunriseHour
        }

        // Slice the forecast array from sunset to 2 hours before sunrise
        val startOffset = hoursToSunset
        val endOffset = hoursToEndOfNight

        summary.append("Observing window: Hour +$startOffset (sunset) to Hour +$endOffset (2h before sunrise)\n\n")

        // Add hourly data for the night window
        for (hourOffset in startOffset..minOf(endOffset, forecast.cloudCover.size - 1)) {
            val cloudData = forecast.cloudCover.getOrNull(hourOffset)
            val seeingData = forecast.seeing.getOrNull(hourOffset)
            val transData = forecast.transparency.getOrNull(hourOffset)

            if (cloudData != null && seeingData != null && transData != null) {
                summary.append("Hour +$hourOffset: Cloud=${cloudData.value.actualValue.toInt()}%, ")
                summary.append("Seeing=${seeingData.value.actualValue.toInt()}/5, ")
                summary.append("Transparency=${transData.value.actualValue.toInt()}\n")
            }
        }

        summary.append("\nMoon: ")
        if (skyData?.moon != null) {
            summary.append("${skyData.moon.illumination.toInt()}% illuminated, ")
            summary.append(if (skyData.moon.altitude > 0) "above horizon" else "below horizon")
        } else {
            summary.append("data unavailable")
        }

        summary.append("\n\nBased on this data, is tonight good for astrophotography? ")
        summary.append("Consider that some clouds early are OK if it clears later. ")
        summary.append("Moon above 50% illumination and above horizon is problematic for deep-sky imaging.\n\n")
        summary.append("Respond ONLY with valid JSON in this exact format (no markdown, no code blocks):\n")
        summary.append("{\n")
        summary.append("  \"isGood\": true,\n")
        summary.append("  \"avgCloudCover\": 15.5,\n")
        summary.append("  \"avgSeeing\": 3.2,\n")
        summary.append("  \"avgTransparency\": 10.8,\n")
        summary.append("  \"description\": \"Your 2-3 sentence natural description here\"\n")
        summary.append("}")

        return summary.toString()
    }

    private suspend fun analyzeWithGemini(nightData: String): ObservingConditions = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isEmpty() || apiKey == "\"\"") {
                throw Exception("Gemini API key not configured")
            }

//            val url = URL("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", nightData)
                            })
                        })
                    })
                })
            }

            println("=== GEMINI API REQUEST ===")
            println("URL: $url")
            println("Request body:")
            println(requestBody.toString(2))
            println("==========================")

            // Write request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            // Read response
            val responseCode = connection.responseCode
            println("=== GEMINI API RESPONSE ===")
            println("Response code: $responseCode")

            if (responseCode != 200) {
                val errorStream = connection.errorStream
                val errorText = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "Unknown error"
                }
                println("Error response:")
                println(errorText)
                println("===========================")
                throw Exception("API returned code $responseCode: $errorText")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            println("Success response:")
            println(response)
            println("===========================")

            val jsonResponse = JSONObject(response)
            val candidates = jsonResponse.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textContent = parts.getJSONObject(0).getString("text")

            println("=== EXTRACTED TEXT ===")
            println(textContent)
            println("======================")

            // Parse the JSON response from Gemini
            val cleanJson = textContent.replace("```json", "").replace("```", "").trim()

            println("=== CLEANED JSON ===")
            println(cleanJson)
            println("====================")

            val analysis = JSONObject(cleanJson)

            ObservingConditions(
                isGood = analysis.getBoolean("isGood"),
                cloudCover = analysis.getDouble("avgCloudCover"),
                seeing = analysis.getDouble("avgSeeing"),
                transparency = analysis.getDouble("avgTransparency"),
                moonIllumination = null,
                moonAltitude = null,
                message = analysis.getString("description")
            )

        } catch (e: Exception) {
            println("=== GEMINI API ERROR ===")
            println("Error: ${e.message}")
            e.printStackTrace()
            println("========================")
            // Fallback to simple average if API fails
            fallbackAnalysis(e.message ?: "Unknown error")
        }
    }

    private fun fallbackAnalysis(errorMessage: String): ObservingConditions {
        return ObservingConditions(
            isGood = false,
            cloudCover = 50.0,
            seeing = 2.0,
            transparency = 15.0,
            moonIllumination = null,
            moonAltitude = null,
            message = "Unable to analyze conditions. Error: $errorMessage"
        )
    }
}