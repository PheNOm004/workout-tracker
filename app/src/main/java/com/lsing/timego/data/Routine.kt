package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [daysOfWeek] stores java.time.DayOfWeek enum names (e.g. "WEDNESDAY"), via the same
 *  Converters.fromStringList/toStringList pair Exercise.muscleGroups already uses -- same List<String>
 *  type, same converter, no new Room TypeConverter needed.
 *
 *  Deliberately NOT annotated with @ColumnInfo(defaultValue=...) -- see Exercise.category's
 *  comment; same real-device-confirmed Room limitation applies here. */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val daysOfWeek: List<String> = emptyList(),
    /** Null for a user-created routine. Non-null identifies which seeded [SeedRoutines] program this
     *  routine belongs to (e.g. "ppl") -- see [WorkoutRepository.seedMissingRoutines]. Never set by
     *  user-facing routine creation. */
    val programId: String? = null,
    /** Only meaningful for programId == "calisthenics_progression" -- one of CalisthenicsTier's
     *  names ("BEGINNER"/"INTERMEDIATE"/"ADVANCED"). Null for every other program. */
    val tier: String? = null,
)
