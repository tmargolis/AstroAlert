package com.toddmargolis.astroalert.data.models

import com.google.gson.annotations.SerializedName

data class AstrosphericForecast(
    @SerializedName("TimeZone") val timeZone: String,
    @SerializedName("UTCMinuteOffset") val utcMinuteOffset: Int,
    @SerializedName("ModelTime") val modelTime: String,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("LocalStartTime") val localStartTime: String,
    @SerializedName("UTCStartTime") val utcStartTime: String,
    @SerializedName("APICreditUsedToday") val apiCreditUsedToday: Int,
    @SerializedName("RDPS_CloudCover") val cloudCover: List<HourData>,
    @SerializedName("Astrospheric_Transparency") val transparency: List<HourData>,
    @SerializedName("Astrospheric_Seeing") val seeing: List<HourData>,
    @SerializedName("RDPS_Temperature") val temperature: List<HourData>?,
    @SerializedName("RDPS_WindVelocity") val windVelocity: List<HourData>?
)

data class HourData(
    @SerializedName("Value") val value: ValueData,
    @SerializedName("HourOffset") val hourOffset: Int
)

data class ValueData(
    @SerializedName("ValueColor") val valueColor: String,
    @SerializedName("ActualValue") val actualValue: Double
)

data class SkyData(
    @SerializedName("Time") val time: String,
    @SerializedName("APICreditUsedToday") val apiCreditUsedToday: Int,
    @SerializedName("Moon") val moon: MoonLocation,
    @SerializedName("Sun") val sun: SunLocation
)

data class MoonLocation(
    @SerializedName("Altitude") val altitude: Double,
    @SerializedName("Azimuth") val azimuth: Double,
    @SerializedName("Illumination") val illumination: Double,
    @SerializedName("Phase") val phase: Double
)

data class SunLocation(
    @SerializedName("Altitude") val altitude: Double,
    @SerializedName("Azimuth") val azimuth: Double
)