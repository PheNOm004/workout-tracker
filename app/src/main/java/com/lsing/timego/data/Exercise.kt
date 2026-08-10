package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [muscleGroups] stores MuscleGroup enum names as strings (via Converters' fromStringList/
 *  toStringList), not the enum type directly, so Room's converter resolution stays unambiguous.
 *  [category] and [loggingType] store their enum names the same way.
 *
 *  Deliberately NOT annotated with @ColumnInfo(defaultValue=...) even though MIGRATION_1_2/
 *  MIGRATION_4_5 add these columns via `ALTER TABLE ... DEFAULT '...'` -- confirmed on a real
 *  device that Room's schema reader doesn't reflect an ALTER-added column's DEFAULT back through
 *  PRAGMA table_info in a way its validator accepts, so declaring the annotation makes Room
 *  reject every real migrated install with "Migration didn't properly handle: exercises" on
 *  open. Room still enforces NOT NULL at the Kotlin/insert level via these fields' non-null
 *  types either way. */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<String>,
    val isCustom: Boolean,
    val category: String = ExerciseCategory.STRENGTH.name,
    val loggingType: String = LoggingType.WEIGHT_REPS.name,
)
