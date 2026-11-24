package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routine_exercises", indices = [Index("routineId")])
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    /** Optional note shown under the set card for this exercise in this routine. Local only: kept in backups, never sent by account sync. */
    val note: String? = null,
)
