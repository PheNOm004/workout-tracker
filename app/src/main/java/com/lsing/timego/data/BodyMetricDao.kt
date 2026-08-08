package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Insert
    suspend fun insert(metric: BodyMetric): Long

    @Query("SELECT * FROM body_metrics ORDER BY date")
    fun observeAll(): Flow<List<BodyMetric>>
}
