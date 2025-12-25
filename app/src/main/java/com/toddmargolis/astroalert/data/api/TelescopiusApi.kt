package com.toddmargolis.astroalert.data.api

import com.google.gson.annotations.SerializedName
import com.toddmargolis.astroalert.data.models.*
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface TelescopiusApi {
    @GET("v2.0/quote-of-the-day")
    suspend fun getQuote(@Header("Authorization") auth: String): Quote

    @GET("v2.0/targets/lists")
    suspend fun getTargetLists(@Header("Authorization") auth: String): List<TelescopiusTargetListSummary>

    @GET("v2.0/targets/lists/{id}")
    suspend fun getTargetList(
        @Path("id") id: String,
        @Header("Authorization") auth: String
    ): TelescopiusTargetListDetail

    @GET("v2.0/targets/highlights")
    suspend fun getHighlights(@Header("Authorization") auth: String): TargetHighlights
}

// Summary list (without target details)
data class TelescopiusTargetListSummary(
    val id: String?,
    val name: String?
)

// Detailed list (with targets)
data class TelescopiusTargetListDetail(
    val id: String?,
    val name: String?,
    val username: String?,
    val objects: List<TargetObject>?
)

data class TargetObject(
    val name: String,
    val coordinates: Coordinates?,
    @SerializedName("size_deg") val sizeDeg: Double?,
    @SerializedName("position_angle_east") val positionAngleEast: Double?,
    val notes: String?
)

data class Coordinates(
    val ra: Double,
    val dec: Double
)