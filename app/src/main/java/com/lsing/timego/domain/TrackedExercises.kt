package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession

/** Exercises that have at least one logged working set, most recently trained first. The Progress
 *  "By Exercise" picker uses this so the 500+ seeded exercises you have never done don't bury the few
 *  that have a record to show. Warm-up sets don't count. */
fun trackedExercises(
    setLogs: List<SetLog>,
    sessions: List<WorkoutSession>,
    exercises: List<Exercise>,
): List<Exercise> {
    val startBySession = sessions.associate { it.id to it.startEpochMillis }
    val lastUsed = HashMap<Long, Long>()
    for (log in setLogs) {
        if (log.isWarmup) continue
        val startedAt = startBySession[log.sessionId] ?: continue
        lastUsed.merge(log.exerciseId, startedAt, ::maxOf)
    }
    return exercises.filter { it.id in lastUsed }.sortedByDescending { lastUsed.getValue(it.id) }
}
