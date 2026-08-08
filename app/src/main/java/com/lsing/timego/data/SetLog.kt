package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [targetReps] is the rep count the user was aiming for on this set -- the overload suggester
 *  compares [reps] against it to detect a missed target. [durationMinutes]/[distanceKm] are used
 *  instead of [weightKg]/[reps] for CARDIO/WARMUP exercises (both null for strength/calisthenics
 *  sets, [weightKg]/[reps] are 0.0/0 sentinels for cardio/warmup sets -- callers branch on the
 *  logged exercise's category, never on null-checking these fields, to know which pair applies). */
@Entity(tableName = "set_logs")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val targetReps: Int,
    val loggedAtEpochMillis: Long,
    val durationMinutes: Double? = null,
    val distanceKm: Double? = null,
)
