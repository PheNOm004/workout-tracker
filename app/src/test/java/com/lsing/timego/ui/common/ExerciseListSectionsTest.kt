package com.lsing.timego.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseListSectionsTest {

    @Test
    fun `formatMuscleGroupList groups related detailed tags into compact regions`() {
        assertEquals("Back & Arms", formatMuscleGroupList(setOf("LATS", "UPPER_BACK", "BICEPS", "FOREARMS")))
    }

    @Test
    fun `formatMuscleGroupList joins three compact regions with commas and a trailing ampersand`() {
        assertEquals("Chest, Shoulders & Arms", formatMuscleGroupList(setOf("CHEST", "FRONT_DELTS", "TRICEPS")))
    }

    @Test
    fun `formatMuscleGroupList orders compact regions not input order`() {
        assertEquals("Chest & Arms", formatMuscleGroupList(setOf("TRICEPS", "CHEST")))
    }

    @Test
    fun `formatMuscleGroupList does not call an upper body session Full Body`() {
        assertEquals("Chest, Back, Shoulders & Arms", formatMuscleGroupList(setOf("CHEST", "LATS", "REAR_DELTS", "TRICEPS", "FOREARMS")))
    }

    @Test
    fun `formatMuscleGroupList keeps shoulders and legs as distinct regions`() {
        assertEquals(
            "Shoulders & Legs",
            formatMuscleGroupList(setOf("FRONT_DELTS", "SIDE_DELTS", "REAR_DELTS", "QUADS", "HAMSTRINGS", "GLUTES", "CALVES")),
        )
    }

    @Test
    fun `formatMuscleGroupList calls an explicit full body tag Full Body`() {
        assertEquals("Full Body", formatMuscleGroupList(setOf("FULL_BODY", "LATS")))
    }

    @Test
    fun `formatMuscleGroupList calls upper lower and core together Full Body`() {
        assertEquals("Full Body", formatMuscleGroupList(setOf("CHEST", "QUADS", "ABS")))
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
        assertEquals("Back & Arms", sessionDayLabel(setOf("LATS", "UPPER_BACK", "BICEPS", "FOREARMS"), isCardioOnly = false))
    }

    @Test
    fun `sessionDayLabel prefers Cardio for a cardio-only session even when tags exist`() {
        assertEquals("Cardio", sessionDayLabel(setOf("FULL_BODY"), isCardioOnly = true))
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
