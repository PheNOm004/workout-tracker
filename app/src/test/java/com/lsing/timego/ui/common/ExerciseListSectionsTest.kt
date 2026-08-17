package com.lsing.timego.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseListSectionsTest {

    @Test
    fun `formatMuscleGroupList joins two groups with an ampersand`() {
        assertEquals("Chest & Triceps", formatMuscleGroupList(setOf("CHEST", "TRICEPS")))
    }

    @Test
    fun `formatMuscleGroupList joins three groups with commas and a trailing ampersand`() {
        assertEquals("Chest, Front Delts & Triceps", formatMuscleGroupList(setOf("CHEST", "FRONT_DELTS", "TRICEPS")))
    }

    @Test
    fun `formatMuscleGroupList orders by anatomical declaration order not input order`() {
        assertEquals("Chest & Triceps", formatMuscleGroupList(setOf("TRICEPS", "CHEST")))
    }

    @Test
    fun `formatMuscleGroupList collapses four or more groups to Full Body`() {
        assertEquals("Full Body", formatMuscleGroupList(setOf("CHEST", "TRICEPS", "QUADS", "CALVES")))
    }

    @Test
    fun `formatMuscleGroupList returns empty string for an empty set`() {
        assertEquals("", formatMuscleGroupList(emptySet()))
    }

    @Test
    fun `formatMuscleGroupList dedupes repeated group names`() {
        assertEquals("Chest", formatMuscleGroupList(listOf("CHEST", "CHEST")))
    }

    @Test
    fun `sessionDayLabel returns the joined muscle-group label when non-empty`() {
        assertEquals("Chest & Triceps", sessionDayLabel(setOf("CHEST", "TRICEPS"), isCardioOnly = false))
    }

    @Test
    fun `sessionDayLabel falls back to Cardio when empty and cardio-only`() {
        assertEquals("Cardio", sessionDayLabel(emptySet(), isCardioOnly = true))
    }

    @Test
    fun `sessionDayLabel falls back to Light Session when empty and not cardio-only`() {
        assertEquals("Light Session", sessionDayLabel(emptySet(), isCardioOnly = false))
    }
}
