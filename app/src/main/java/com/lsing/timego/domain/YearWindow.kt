package com.lsing.timego.domain

import java.time.LocalDate
import java.time.YearMonth

const val CALENDAR_MONTHS_BACK = 24
const val CALENDAR_MONTHS_FORWARD = 3

/** Every month the continuous calendar renders, oldest first: [today - monthsBack, today + monthsForward]. */
fun calendarMonthWindow(
    today: LocalDate = LocalDate.now(),
    monthsBack: Int = CALENDAR_MONTHS_BACK,
    monthsForward: Int = CALENDAR_MONTHS_FORWARD,
): List<YearMonth> {
    val start = YearMonth.from(today).minusMonths(monthsBack.toLong())
    val end = YearMonth.from(today).plusMonths(monthsForward.toLong())
    return generateSequence(start) { it.plusMonths(1) }.takeWhile { !it.isAfter(end) }.toList()
}

/** Distinct calendar years the continuous calendar's month window touches — can be up to 4 (e.g. viewed in December). */
fun calendarYearsSpanned(today: LocalDate = LocalDate.now()): Set<Int> =
    calendarMonthWindow(today).map { it.year }.toSet()

/** Years eagerly loaded at startup: near-term history plus streak continuity across a Jan 1 boundary. */
fun defaultLoadedYears(today: LocalDate = LocalDate.now()): Set<Int> =
    setOf(today.year - 1, today.year, today.year + 1)
