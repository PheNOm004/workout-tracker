package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [targetReps] is the rep count the user was aiming for on this set -- the overload suggester
 *  (Task 6) compares [reps] against it to detect a missed target. */
@Entity(tableName = "set_logs")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val targetReps: Int,
    val loggedAtEpochMillis: Long,
)
