package com.lsing.timego.report

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class ReportPeriod(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
    fun previous(): ReportPeriod {
        val days = endInclusive.toEpochDay() - start.toEpochDay() + 1
        return ReportPeriod(start.minusDays(days), start.minusDays(1))
    }

    companion object {
        fun weekContaining(date: LocalDate): ReportPeriod {
            val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return ReportPeriod(start, start.plusDays(6))
        }
        fun month(month: YearMonth): ReportPeriod = ReportPeriod(month.atDay(1), month.atEndOfMonth())
    }
}
