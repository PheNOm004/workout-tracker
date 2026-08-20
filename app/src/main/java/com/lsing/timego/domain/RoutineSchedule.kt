package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import com.lsing.timego.data.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun routinesForToday(routines: List<Routine>, today: DayOfWeek): List<Routine> =
    routines.filter { today.name in it.daysOfWeek }

/** Latest date of a *closed* session per routine id. A routine's still-active session doesn't
 *  count as "completed" yet (endEpochMillis == null is excluded), matching the same closed-
 *  session convention used elsewhere (e.g. WorkoutRepository.deleteSession,
 *  RoutinesViewModel.sessionHistory). A routine id absent from the returned map has never been
 *  completed -- callers must not assume every routine has an entry. */
fun routineLastCompletedDates(sessions: List<WorkoutSession>): Map<Long, LocalDate> =
    sessions
        .filter { it.endEpochMillis != null && it.routineId != null }
        .groupBy { it.routineId!! }
        .mapValues { (_, group) -> group.maxOf { it.date } }

/** "Today" for the same day, "<n>d ago" for a past date, "Never logged" for null (no completed
 *  session exists for this routine yet) -- backs the landing page's routine nudge list. */
fun formatDaysSince(date: LocalDate?, today: LocalDate): String {
    if (date == null) return "Never logged"
    val days = ChronoUnit.DAYS.between(date, today)
    return if (days <= 0) "Today" else "${days}d ago"
}
