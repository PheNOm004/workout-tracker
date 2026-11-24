package com.lsing.timego.domain

import com.lsing.timego.data.RoutineExercise

/** New order for a routine's exercises ([exerciseIds], in order), carrying over each exercise's existing note. */
fun relinkRoutineExercises(routineId: Long, previous: List<RoutineExercise>, exerciseIds: List<Long>): List<RoutineExercise> {
    val notes = previous.associate { it.exerciseId to it.note }
    return exerciseIds.mapIndexed { index, exerciseId ->
        RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index, note = notes[exerciseId])
    }
}
