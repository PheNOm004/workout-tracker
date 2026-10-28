package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession

/** Exercises that have a personal record to show (their id is in [recordedExerciseIds]) and at least one logged
 *  working set, most recently trained first. The Progress "By Exercise" picker uses this so neither the 500+
 *  seeded exercises you have never done nor ones that can't hold a record (stretches, cardio) clutter it.
 *  Warm-up sets don't count. */
fun trackedExercises(
    setLogs: List<SetLog>,
    sessions: List<WorkoutSession>,
    exercises: List<Exercise>,
    recordedExerciseIds: Set<Long>,
): List<Exercise> {
    val startBySession = sessions.associate { it.id to it.startEpochMillis }
    val lastUsed = HashMap<Long, Long>()
    for (log in setLogs) {
        if (log.isWarmup) continue
        val startedAt = startBySession[log.sessionId] ?: continue
        lastUsed.merge(log.exerciseId, startedAt, ::maxOf)
    }
    return exercises.filter { it.id in lastUsed && it.id in recordedExerciseIds }.sortedByDescending { lastUsed.getValue(it.id) }
}
