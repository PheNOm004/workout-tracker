package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [targetReps] is the rep count the user was aiming for on this set -- the overload suggester
 *  compares [reps] against it to detect a missed target. [durationMinutes]/[distanceKm] are used
 *  instead of [weightKg]/[reps] for CARDIO/WARMUP exercises; [holdSeconds]/[targetHoldSeconds]
 *  are used instead for HOLD exercises (see LoggingType). [weightKg]/[reps] are 0.0/0 sentinels
 *  whenever a different pair applies -- callers branch on the logged exercise's loggingType,
 *  never on null-checking these fields, to know which pair is real. [isWarmup] marks a set as
 *  excluded from the overload suggester's working-set baseline (see [sessionWorkingSetHistory])
 *  -- only meaningful for WEIGHT_REPS/HOLD sets, which are the only logging types with a
 *  suggester; CARDIO/WARMUP-category sets never set it. */
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
    val holdSeconds: Int? = null,
    val targetHoldSeconds: Int? = null,
    val isWarmup: Boolean = false,
    /** Only set for CALISTHENICS exercises -- the added-weight-only k typed at entry, kept
     *  alongside [weightKg] (which stays the full bodyweight+k total, for 1RM/PR/suggester math)
     *  purely so display can format it as "BW + k" instead of a raw absolute number. */
    val addedWeightKg: Double? = null,
)
