package com.toddmargolis.astroalert.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface SunriseSunsetApi {
    @GET("json")
    suspend fun getSunriseSunset(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("date") date: String = "today",
        @Query("formatted") formatted: Int = 0
    ): SunriseSunsetResponse
}

data class SunriseSunsetResponse(
    val results: SunTimes,
    val status: String
)

data class SunTimes(
    @SerializedName("sunrise") val sunrise: String,  // ISO 8601 format
    @SerializedName("sunset") val sunset: String,
    @SerializedName("solar_noon") val solarNoon: String,
    @SerializedName("day_length") val dayLength: Int,
    @SerializedName("civil_twilight_begin") val civilTwilightBegin: String,
    @SerializedName("civil_twilight_end") val civilTwilightEnd: String,
    @SerializedName("nautical_twilight_begin") val nauticalTwilightBegin: String,
    @SerializedName("nautical_twilight_end") val nauticalTwilightEnd: String,
    @SerializedName("astronomical_twilight_begin") val astronomicalTwilightBegin: String,
    @SerializedName("astronomical_twilight_end") val astronomicalTwilightEnd: String
)