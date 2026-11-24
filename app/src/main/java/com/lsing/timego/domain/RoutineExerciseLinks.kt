package com.lsing.timego.domain

import com.lsing.timego.data.RoutineExercise

/** New order for a routine's exercises ([exerciseIds], in order), carrying over each exercise's existing note. */
fun relinkRoutineExercises(
    routineId: Long,
    previous: List<RoutineExercise>,
    exerciseIds: List<Long>,
    noteOverrides: Map<Long, String>? = null,
): List<RoutineExercise> {
    val notes = previous.associate { it.exerciseId to it.note }
    return exerciseIds.mapIndexed { index, exerciseId ->
        val note = if (noteOverrides != null) normalizeNote(noteOverrides[exerciseId]) else notes[exerciseId]
        RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index, note = note)
    }
}

/** Trimmed note, or null when blank, so an empty box clears the note. */
fun normalizeNote(text: String?): String? = text?.trim()?.takeIf { it.isNotEmpty() }
