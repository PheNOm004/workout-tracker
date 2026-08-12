package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** [date] is derived from [startEpochMillis] at creation and never changes afterward -- kept as
 *  its own column (rather than computed on read) because the heatmap and other date-grouped
 *  queries already group by it. [endEpochMillis] null means the session is still active; at most
 *  one session should have a null [endEpochMillis] at a time (an app-level invariant, not a DB
 *  constraint -- enforced by the landing page being the only place a new session is created, and
 *  it only renders when no active session exists). */
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val routineId: Long?,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
)
