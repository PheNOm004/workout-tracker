package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate
import kotlin.math.abs

/** A routine in a candidate program: its exercises and how many times a week it runs (flexible routines count once). */
data class ProjectedRoutine(val exerciseNames: List<String>, val timesPerWeek: Int = 1)

data class MuscleDelta(val muscle: String, val currentWeekly: Double, val projectedWeekly: Double) {
    val delta: Double get() = projectedWeekly - currentWeekly
}

data class ProgramDiff(val projected: Map<String, Double>, val current: Map<String, Double>, val gainers: List<MuscleDelta>, val drops: List<MuscleDelta>)

const val DEFAULT_WORKING_SETS_PER_EXERCISE = 3
private const val MIN_NOTABLE_DELTA = 0.5
private const val TOP_N = 3

/** Weekly working sets per muscle if the program were followed as written: each exercise gets
 *  [setsPerExercise] sets per session, scaled by the routine's weekly frequency, credited to every muscle
 *  the exercise lists at its muscleWeights share (100 when unlisted). Exercise names missing from the library are skipped. */
fun projectedWeeklySets(
    routines: List<ProjectedRoutine>,
    exercisesByName: Map<String, Exercise>,
    setsPerExercise: Int = DEFAULT_WORKING_SETS_PER_EXERCISE,
): Map<String, Double> {
    val totals = mutableMapOf<String, Double>()
    for (routine in routines) {
        val times = routine.timesPerWeek.coerceAtLeast(1)
        for (name in routine.exerciseNames) {
            val exercise = exercisesByName[name] ?: continue
            for (group in exercise.muscleGroups) {
                totals[group] = (totals[group] ?: 0.0) + setsPerExercise * times * (exercise.muscleWeights[group] ?: 100) / 100.0
            }
        }
    }
    return totals
}

/** Weekly average of working sets per muscle over the [days] ending [today], on the same credit rules as the projection. */
fun trailingWeeklySets(
    sets: List<SetLog>,
    sessions: List<WorkoutSession>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
    days: Int = 30,
): Map<String, Double> {
    val sessionDateById = sessions.associate { it.id to it.date }
    val since = today.minusDays(days.toLong() - 1)
    val totals = mutableMapOf<String, Double>()
    for (log in sets) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (!isTrainingSet(log, exercise)) continue
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since) || date.isAfter(today)) continue
        for (group in exercise.muscleGroups) {
            totals[group] = (totals[group] ?: 0.0) + (exercise.muscleWeights[group] ?: 100) / 100.0
        }
    }
    val weeks = days / 7.0
    return totals.mapValues { it.value / weeks }
}

/** The three muscles that gain the most and the three that lose the most weekly sets; shifts under half a set are ignored. */
fun diffWeeklySets(projected: Map<String, Double>, current: Map<String, Double>): ProgramDiff {
    val deltas = (projected.keys + current.keys).map { MuscleDelta(it, current[it] ?: 0.0, projected[it] ?: 0.0) }
        .filter { abs(it.delta) >= MIN_NOTABLE_DELTA }
    return ProgramDiff(
        projected = projected,
        current = current,
        gainers = deltas.filter { it.delta > 0 }.sortedByDescending { it.delta }.take(TOP_N),
        drops = deltas.filter { it.delta < 0 }.sortedBy { it.delta }.take(TOP_N),
    )
}
