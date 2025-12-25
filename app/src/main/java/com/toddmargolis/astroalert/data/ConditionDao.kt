package com.toddmargolis.astroalert.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.toddmargolis.astroalert.data.models.ConditionCheckResult

@Dao
interface ConditionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: ConditionCheckResult)

    @Query("SELECT * FROM condition_checks ORDER BY checkTime DESC LIMIT 20")
    suspend fun getRecentChecks(): List<ConditionCheckResult>

    @Query("SELECT * FROM condition_checks WHERE locationName = :location ORDER BY checkTime DESC LIMIT 1")
    suspend fun getLatestCheckForLocation(location: String): ConditionCheckResult?

    @Query("DELETE FROM condition_checks WHERE checkTime < :cutoffTime")
    suspend fun deleteOldChecks(cutoffTime: Long)
}