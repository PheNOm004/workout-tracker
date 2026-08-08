package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun lastTrainedDatesByMuscleGroup(
    setLogs: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
): Map<String, LocalDate> {
    val result = mutableMapOf<String, LocalDate>()
    for (log in setLogs) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        val date = sessionDateById[log.sessionId] ?: continue
        for (group in exercise.muscleGroups) {
            val current = result[group]
            if (current == null || date.isAfter(current)) result[group] = date
        }
    }
    return result
}

fun untrainedMuscleGroups(
    allGroups: List<String>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
    thresholdDays: Int = 7,
): List<String> = allGroups.filter { group ->
    val last = lastTrainedByGroup[group]
    last == null || ChronoUnit.DAYS.between(last, today) >= thresholdDays
}
