package com.lsing.timego.domain

import com.lsing.timego.data.RoutineExercise
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineExerciseLinksTest {
    private val previous = listOf(
        RoutineExercise(id = 1, routineId = 5, exerciseId = 10, orderIndex = 0, note = "seat 4"),
        RoutineExercise(id = 2, routineId = 5, exerciseId = 20, orderIndex = 1),
    )

    @Test fun reorderKeepsEachExercisesNote() {
        val result = relinkRoutineExercises(5, previous, listOf(20L, 10L))
        assertEquals(listOf(20L, 10L), result.map { it.exerciseId })
        assertEquals(listOf<String?>(null, "seat 4"), result.map { it.note })
        assertEquals(listOf(0, 1), result.map { it.orderIndex })
    }

    @Test fun newExerciseGetsNoNoteAndRemovedNoteIsDropped() {
        val result = relinkRoutineExercises(5, previous, listOf(30L))
        assertEquals(listOf<String?>(null), result.map { it.note })
    }
}

class RoutineNoteOverrideTest {
    private val previous = listOf(RoutineExercise(id = 1, routineId = 5, exerciseId = 10, orderIndex = 0, note = "seat 4"))

    @Test fun overridesReplaceTrimAndClearNotes() {
        val result = relinkRoutineExercises(5, previous, listOf(10L, 20L), mapOf(10L to "  seat 5  ", 20L to "   "))
        org.junit.Assert.assertEquals(listOf<String?>("seat 5", null), result.map { it.note })
    }

    @Test fun nullOverridesKeepExistingNotes() {
        val result = relinkRoutineExercises(5, previous, listOf(10L), null)
        org.junit.Assert.assertEquals(listOf<String?>("seat 4"), result.map { it.note })
    }

    @Test fun normalizeNoteTrimsAndDropsBlank() {
        org.junit.Assert.assertNull(normalizeNote("  "))
        org.junit.Assert.assertEquals("a", normalizeNote(" a "))
    }
}
