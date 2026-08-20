package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import com.lsing.timego.data.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate

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
