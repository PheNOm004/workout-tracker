package com.lsing.timego.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordsOverviewTest {
    @Test
    fun newestFirstAndFlaggedFromSinceInclusive() {
        val day = LocalDate.of(2026, 11, 1)
        fun record(id: Long, on: LocalDate) = PersonalRecord(id, PrType.BEST_SET, 50.0, 8.0, on)
        val result = recordsOverview(listOf(record(1, day.minusDays(1)), record(2, day), record(3, day.plusDays(1))), day)
        assertEquals(listOf(3L, 2L, 1L), result.map { it.record.exerciseId })
        assertEquals(listOf(true, true, false), result.map { it.isNew })
    }
}
