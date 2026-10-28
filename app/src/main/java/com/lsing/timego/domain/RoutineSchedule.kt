package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun routinesForToday(routines: List<Routine>, today: DayOfWeek): List<Routine> =
    routines.filter { today.name in it.daysOfWeek }

/** Routines that have no specific days assigned and can be followed flexibly on any day. */
fun flexibleRoutines(routines: List<Routine>): List<Routine> =
    routines.filter { it.daysOfWeek.isEmpty() }

/** Selects the next flexible routine in rotation based on least-recently completed date.
 *  Routines that have never been completed are prioritized first. */
fun nextFlexibleRoutineInRotation(
    flexibleRoutines: List<Routine>,
    routineLastCompleted: Map<Long, LocalDate>,
): Routine? =
    flexibleRoutines.minByOrNull { routineLastCompleted[it.id]?.toEpochDay() ?: Long.MIN_VALUE }

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

/** Picks the next flexible routine by muscle recovery rather than routine history, so freeform sessions
 *  (and other routines) that already trained a routine's muscles count. Each routine scores the mean
 *  staleness of its primary groups (capped at [STALENESS_CAP_DAYS], never trained = cap); any group still
 *  inside [MIN_RECOVERY_DAYS] costs [RECOVERY_PENALTY]. Ties fall back to least-recently-completed. */
private const val STALENESS_CAP_DAYS = 14
private const val RECOVERY_PENALTY = 100

fun nextFlexibleRoutineByRecovery(
    flexibleRoutines: List<Routine>,
    routinePrimaryGroups: Map<Long, Set<String>>,
    lastWorkedByGroup: Map<String, LocalDate>,
    routineLastCompleted: Map<Long, LocalDate>,
    today: LocalDate,
): Routine? {
    fun score(routine: Routine): Double {
        val groups = routinePrimaryGroups[routine.id].orEmpty()
        if (groups.isEmpty()) return 0.0
        return groups.sumOf { group ->
            val days = lastWorkedByGroup[group]?.let { ChronoUnit.DAYS.between(it, today).toInt() }
            when {
                days == null -> STALENESS_CAP_DAYS
                days < MIN_RECOVERY_DAYS -> days - RECOVERY_PENALTY
                else -> minOf(days, STALENESS_CAP_DAYS)
            }
        }.toDouble() / groups.size
    }
    return flexibleRoutines.maxWithOrNull(
        // Whole-day buckets: overlapping routines (Upper A/B) score within a fraction of a day of each
        // other, which must read as a tie so the least-recently-done routine decides.
        compareBy<Routine> { score(it).roundToInt() }
            .thenBy { routineLastCompleted[it.id]?.let { d -> ChronoUnit.DAYS.between(d, today) } ?: Long.MAX_VALUE },
    )
}

private const val INFERRED_MIN_COVERAGE = 0.5

/** Credits each finished freeform session (no routine tag) to the routine it most resembles, so a
 *  forgotten tag doesn't hide what was done. A routine exercise counts as covered by a logged exercise
 *  with the same id/name, else one with identical primary muscles (one-to-one). A session maps to its
 *  best routine only when that routine is covered >= 50% and no other routine ties it. */
fun inferredRoutineLastDone(
    sessions: List<WorkoutSession>,
    setLogs: List<SetLog>,
    routineExercises: Map<Long, List<Exercise>>,
    exercisesById: Map<Long, Exercise>,
): Map<Long, LocalDate> {
    val logsBySession = setLogs.groupBy { it.sessionId }
    val result = mutableMapOf<Long, LocalDate>()
    for (session in sessions) {
        if (session.routineId != null || session.endEpochMillis == null) continue
        val done = logsBySession[session.id].orEmpty()
            .mapNotNull { log -> exercisesById[log.exerciseId]?.takeIf { isTrainingSet(log, it) } }
            .distinctBy { it.id }
        if (done.isEmpty()) continue
        val scored = routineExercises.filterValues { it.isNotEmpty() }.map { (id, steps) -> id to routineCoverage(steps, done) }
        val best = scored.maxOfOrNull { it.second } ?: continue
        val winners = scored.filter { it.second == best }
        if (best < INFERRED_MIN_COVERAGE || winners.size != 1) continue
        result.merge(winners.single().first, session.date) { a, b -> maxOf(a, b) }
    }
    return result
}

private fun routineCoverage(steps: List<Exercise>, done: List<Exercise>): Double {
    val free = done.toMutableList()
    var matched = 0
    val unmatched = mutableListOf<Exercise>()
    for (step in steps) {
        val hit = free.firstOrNull { it.id == step.id || it.name == step.name }
        if (hit != null) { free.remove(hit); matched++ } else unmatched += step
    }
    for (step in unmatched) {
        val groups = primaryMuscleGroups(step)
        val hit = free.firstOrNull { groups.isNotEmpty() && primaryMuscleGroups(it) == groups }
        if (hit != null) { free.remove(hit); matched++ }
    }
    return matched.toDouble() / steps.size
}
