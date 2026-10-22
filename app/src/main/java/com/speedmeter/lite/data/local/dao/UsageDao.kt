package com.speedmeter.lite.data.local.dao

import androidx.room.*
import com.speedmeter.lite.data.local.entity.DailyUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT 30")
    fun getLast30Days(): Flow<List<DailyUsage>>

    @Query("SELECT * FROM daily_usage WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyUsage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: DailyUsage)

    @Query("DELETE FROM daily_usage WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)
}
