package com.lsing.timego.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** [daysOfWeek] stores java.time.DayOfWeek enum names (e.g. "WEDNESDAY"), via the same
 *  Converters.fromStringList/toStringList pair Exercise.muscleGroups already uses -- same List<String>
 *  type, same converter, no new Room TypeConverter needed. The defaultValue here must match
 *  MIGRATION_1_2's `ALTER TABLE ... DEFAULT ''`, or Room's schema validation fails on upgrade
 *  for installs that ran the migration (see TimeGoDatabase.kt). */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "''") val daysOfWeek: List<String> = emptyList(),
)
