package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** [heightCm] doesn't change often -- most entries will leave it null and BMI calculations fall
 *  back to the most recent non-null value logged (see WorkoutRepository.latestHeightCm). */
@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double?,
    val waistCm: Double?,
    val heightCm: Double? = null,
)
