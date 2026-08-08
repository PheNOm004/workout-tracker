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

    private companion object {
        const val LIST_DELIMITER = ""
    }
}
