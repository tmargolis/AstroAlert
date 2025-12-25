package com.toddmargolis.astroalert.data.models

import com.google.gson.annotations.SerializedName

data class TelescopiusTargetList(
    val id: String?,
    val name: String?,
    val targets: List<TelescopiusTarget>?
)

data class TelescopiusTarget(
    val id: String,
    val name: String,
    val type: String?,
    @SerializedName("ra") val rightAscension: Double?,
    @SerializedName("dec") val declination: Double?,
    val magnitude: Double?,
    @SerializedName("con_name") val constellation: String?
)

data class Quote(
    val text: String,
    val author: String
)

data class TargetHighlights(
    val matched: Int,
    @SerializedName("page_results") val pageResults: List<TargetResult>
)

data class TargetResult(
    @SerializedName("object") val targetObject: TargetObject
)

data class TargetObject(
    @SerializedName("main_name") val mainName: String,
    @SerializedName("con_name") val constellationName: String?,
    val types: List<String>?
)