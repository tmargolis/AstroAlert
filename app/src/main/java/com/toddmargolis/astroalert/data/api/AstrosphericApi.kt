package com.toddmargolis.astroalert.data.api

import com.toddmargolis.astroalert.data.models.AstrosphericForecast
import com.toddmargolis.astroalert.data.models.SkyData
import retrofit2.http.Body
import retrofit2.http.POST

interface AstrosphericApi {
    @POST("GetForecastData_V1")
    suspend fun getForecast(
        @Body request: ForecastRequest
    ): AstrosphericForecast

    @POST("GetSky_V1")
    suspend fun getSkyData(
        @Body request: SkyRequest
    ): SkyData
}

data class ForecastRequest(
    val Latitude: Double,
    val Longitude: Double,
    val APIKey: String
)

data class SkyRequest(
    val Latitude: Double,
    val Longitude: Double,
    val MSSinceEpoch: Long,
    val APIKey: String
)