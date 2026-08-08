package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [muscleGroups] stores MuscleGroup enum names as strings (via Converters' fromStringList/
 *  toStringList), not the enum type directly, so Room's converter resolution stays unambiguous.
 *  [category] stores an ExerciseCategory enum name the same way. */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<String>,
    val isCustom: Boolean,
    val category: String = ExerciseCategory.STRENGTH.name,
)
