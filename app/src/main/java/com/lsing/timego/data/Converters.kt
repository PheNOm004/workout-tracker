package com.lsing.timego.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    /** For a String list column (e.g. an Exercise's muscle-group tags). ASCII unit separator
     *  (0x1F), not comma, since tag text could legitimately contain a comma. Lifted from HeatP's
     *  sub-option-list converter, which needed the same non-comma delimiter for the same reason. */
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_DELIMITER)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(LIST_DELIMITER)

    /** For Exercise.muscleWeights: MuscleGroup name -> 0-100 percentage contribution. One
     *  delimiter level below the list delimiter (ASCII record separator 0x1E between entries,
     *  unit separator 0x1F between an entry's group/weight pair), same non-printable-character
     *  convention as [fromStringList] -- group names and weights can't contain either. */
    @TypeConverter
    fun fromMuscleWeights(value: Map<String, Int>): String =
        value.entries.joinToString(ENTRY_DELIMITER) { (group, weight) -> "$group$PAIR_DELIMITER$weight" }

    @TypeConverter
    fun toMuscleWeights(value: String): Map<String, Int> =
        if (value.isBlank()) {
            emptyMap()
        } else {
            // Skips malformed entries rather than throwing. A TypeConverter runs inside every
            // read of the exercises table, so an unparseable pair here would surface as a crash
            // on app open with no way for the user to recover the row -- dropping the weight
            // degrades to the documented "missing group defaults to full credit" behaviour.
            value.split(ENTRY_DELIMITER).mapNotNull { entry ->
                val parts = entry.split(PAIR_DELIMITER)
                val weight = parts.getOrNull(1)?.toIntOrNull()
                if (parts.size == 2 && weight != null) parts[0] to weight else null
            }.toMap()
        }

    private companion object {
        const val LIST_DELIMITER = ""
        const val ENTRY_DELIMITER = ""
        const val PAIR_DELIMITER = ""
    }
}
