package com.lsing.timego.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class YearWindowTest {
    @Test
    fun `month window spans monthsBack plus monthsForward plus one months`() {
        val window = calendarMonthWindow(LocalDate.of(2026, 7, 26), monthsBack = 24, monthsForward = 3)
        assertEquals(28, window.size)
        assertEquals(YearMonth.of(2024, 7), window.first())
        assertEquals(YearMonth.of(2026, 10), window.last())
    }

    @Test
    fun `december can span four distinct calendar years`() {
        val years = calendarYearsSpanned(LocalDate.of(2026, 12, 15))
        assertEquals(setOf(2024, 2025, 2026, 2027), years)
    }

    @Test
    fun `default loaded years is a three year band around today`() {
        assertEquals(setOf(2025, 2026, 2027), defaultLoadedYears(LocalDate.of(2026, 6, 1)))
    }
}
