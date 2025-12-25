package com.toddmargolis.astroalert.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "condition_checks")
data class ConditionCheckResult(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val locationName: String,
    val checkTime: Long,
    val isGood: Boolean,
    val cloudCover: Double,
    val seeing: Double,
    val transparency: Double,
    val moonIllumination: Double?,
    val moonAltitude: Double?,
    val message: String,
    val recommendedTargets: String? = null  // JSON string of targets
)