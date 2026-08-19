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
    fun `keeps the newest two groups when a third group opens`() {
        val expanded = toggleExpandedExerciseGroupKeys(
            listOf("STRENGTH:CHEST", "STRENGTH:BACK"),
            "STRENGTH:SHOULDERS",
        )

        assertEquals(listOf("STRENGTH:BACK", "STRENGTH:SHOULDERS"), expanded)
    }

    @Test
    fun `collapses only the tapped open group`() {
        val expanded = toggleExpandedExerciseGroupKeys(
            listOf("STRENGTH:CHEST", "STRENGTH:BACK"),
            "STRENGTH:BACK",
        )

        assertEquals(listOf("STRENGTH:CHEST"), expanded)
    }

    @Test
    fun `reopening an evicted group makes it newest`() {
        val expanded = toggleExpandedExerciseGroupKeys(
            listOf("STRENGTH:BACK", "STRENGTH:SHOULDERS"),
            "STRENGTH:CHEST",
        )

        assertEquals(listOf("STRENGTH:SHOULDERS", "STRENGTH:CHEST"), expanded)
    }
}
