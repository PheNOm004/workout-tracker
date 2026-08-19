package com.lsing.timego.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpandedExerciseGroupsTest {
    @Test
    fun `opens a first muscle group`() {
        val expanded = toggleExpandedExerciseGroupKeys(emptyList(), "STRENGTH:CHEST")

        assertEquals(listOf("STRENGTH:CHEST"), expanded)
    }

    @Test
    fun `keeps only the newly opened group when a second group opens`() {
        val expanded = toggleExpandedExerciseGroupKeys(
            listOf("STRENGTH:CHEST"),
            "STRENGTH:BACK",
        )

        assertEquals(listOf("STRENGTH:BACK"), expanded)
    }

    @Test
    fun `collapses only the tapped open group`() {
        val expanded = toggleExpandedExerciseGroupKeys(
            listOf("STRENGTH:CHEST"),
            "STRENGTH:CHEST",
        )

        assertEquals(emptyList<String>(), expanded)
    }

    @Test
    fun `opening a former group replaces the current group`() {
        val expanded = toggleExpandedExerciseGroupKeys(
            listOf("STRENGTH:BACK"),
            "STRENGTH:CHEST",
        )

        assertEquals(listOf("STRENGTH:CHEST"), expanded)
    }
}
