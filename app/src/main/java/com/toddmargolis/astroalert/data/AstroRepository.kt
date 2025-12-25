package com.toddmargolis.astroalert.data

import com.toddmargolis.astroalert.data.api.ApiClient
import com.toddmargolis.astroalert.data.api.ForecastRequest
import com.toddmargolis.astroalert.data.api.SkyRequest
import com.toddmargolis.astroalert.data.api.SunTimes
import com.toddmargolis.astroalert.data.api.TelescopiusTargetListDetail
import com.toddmargolis.astroalert.data.models.Quote
import com.toddmargolis.astroalert.data.models.AstrosphericForecast
import com.toddmargolis.astroalert.data.models.TargetHighlights
import com.toddmargolis.astroalert.data.models.TelescopiusTargetList
import com.toddmargolis.astroalert.data.models.SkyData
import com.toddmargolis.astroalert.data.api.TelescopiusTargetListSummary

class AstroRepository {

    suspend fun getWeatherForecast(latitude: Double, longitude: Double): Result<AstrosphericForecast> {
        return try {
            val request = ForecastRequest(
                Latitude = latitude,
                Longitude = longitude,
                APIKey = ApiClient.astrosphericApiKey
            )
            val forecast = ApiClient.astrosphericApi.getForecast(request)
            Result.success(forecast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuote(): Result<Quote> {
        return try {
            val quote = ApiClient.telescopiusApi.getQuote(ApiClient.telescopiusApiKey)
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTargetLists(): Result<List<TelescopiusTargetListSummary>> {
        return try {
            val lists = ApiClient.telescopiusApi.getTargetLists(ApiClient.telescopiusApiKey)
            Result.success(lists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTargetList(id: String): Result<TelescopiusTargetListDetail> {
        return try {
            val list = ApiClient.telescopiusApi.getTargetList(id, ApiClient.telescopiusApiKey)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHighlights(): Result<TargetHighlights> {
        return try {
            val highlights = ApiClient.telescopiusApi.getHighlights(ApiClient.telescopiusApiKey)
            Result.success(highlights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkyData(latitude: Double, longitude: Double, timeMillis: Long): Result<SkyData> {
        return try {
            val request = SkyRequest(
                Latitude = latitude,
                Longitude = longitude,
                MSSinceEpoch = timeMillis,
                APIKey = ApiClient.astrosphericApiKey
            )
            val skyData = ApiClient.astrosphericApi.getSkyData(request)
            Result.success(skyData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSunTimes(latitude: Double, longitude: Double): Result<SunTimes> {
        return try {
            val response = ApiClient.sunriseSunsetApi.getSunriseSunset(latitude, longitude)
            if (response.status == "OK") {
                Result.success(response.results)
            } else {
                Result.failure(Exception("Failed to get sun times: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}