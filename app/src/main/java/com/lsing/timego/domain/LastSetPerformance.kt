package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession

/**
 * The latest real working set per exercise from completed sessions only. Active-session logs are
 * intentionally excluded so the reference stays anchored to the previous completed workout.
 */
fun lastWorkingSetByExercise(
    setLogs: List<SetLog>,
    sessions: List<WorkoutSession>,
    exercisesById: Map<Long, Exercise>,
): Map<Long, SetLog> {
    val closedSessionIds = sessions
        .asSequence()
        .filter { it.endEpochMillis != null }
        .map { it.id }
        .toSet()

    return setLogs
        .asSequence()
        .filter { log ->
            val exercise = exercisesById[log.exerciseId]
            log.sessionId in closedSessionIds &&
                !log.isWarmup &&
                exercise != null &&
                exercise.category != ExerciseCategory.WARMUP.name &&
                exercise.category != ExerciseCategory.CARDIO.name
        }
        .groupBy { it.exerciseId }
        .mapValues { (_, logs) -> logs.maxBy { it.loggedAtEpochMillis } }
}
