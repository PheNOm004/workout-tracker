package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate

private const val BASELINE_WINDOW_DAYS = 56

private val anatomicalMuscleGroups = MuscleGroup.entries
    .filterNot { it == MuscleGroup.FULL_BODY }
    .map { it.name }

private fun sessionsTouchingGroup(
    group: String,
    sessions: List<WorkoutSession>,
    setsBySession: Map<Long, List<SetLog>>,
    exercises: List<Exercise>,
    sinceDate: LocalDate,
    today: LocalDate,
): Int =
    sessions.count { session ->
        !session.date.isBefore(sinceDate) &&
            !session.date.isAfter(today) &&
            group in muscleGroupsWorkedInSession(session.id, setsBySession[session.id].orEmpty(), exercises)
    }

/**
 * Rates each muscle group against its own trailing eight-week session cadence rather than raw
 * moved volume. A group at or above its usual cadence reaches the full radar spoke.
 */
fun frequencyDistributionForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float> {
    val baselineSince = today.minusDays((BASELINE_WINDOW_DAYS - 1).toLong())
    val selectedSince = timeframe.sinceDate(sessions.minOfOrNull { it.date }, today)
    val baselineWeeks = BASELINE_WINDOW_DAYS / 7.0
    val selectedWeeks = (today.toEpochDay() - selectedSince.toEpochDay() + 1) / 7.0
    val setsBySession = sets.groupBy { it.sessionId }
    val exercises = exercisesById.values.toList()

    return anatomicalMuscleGroups.associateWith { group ->
        val baselineSessions = sessionsTouchingGroup(
            group,
            sessions,
            setsBySession,
            exercises,
            baselineSince,
            today,
        )
        val expectedSessions = baselineSessions / baselineWeeks * selectedWeeks
        val actualSessions = sessionsTouchingGroup(
            group,
            sessions,
            setsBySession,
            exercises,
            selectedSince,
            today,
        )

        when {
            expectedSessions <= 0.0 && actualSessions <= 0 -> 0f
            expectedSessions <= 0.0 -> 1f
            else -> (actualSessions / expectedSessions).toFloat().coerceIn(0f, 1f)
        }
    }
}
