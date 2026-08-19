package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpandedExerciseRowsTest {
    @Test
    fun `adds a first expanded row`() {
        val expanded = toggleExpandedExerciseIds(emptyList(), 10L)

        assertEquals(listOf(10L), expanded)
    }

    @Test
    fun `keeps the newest three rows when a fourth row opens`() {
        val expanded = toggleExpandedExerciseIds(listOf(10L, 20L, 30L), 40L)

        assertEquals(listOf(20L, 30L, 40L), expanded)
    }

    @Test
    fun `collapses only the tapped expanded row`() {
        val expanded = toggleExpandedExerciseIds(listOf(10L, 20L, 30L), 20L)

        assertEquals(listOf(10L, 30L), expanded)
    }

    @Test
    fun `reopening a collapsed row makes it the newest open row`() {
        val expanded = toggleExpandedExerciseIds(listOf(20L, 30L, 40L), 10L)

        assertEquals(listOf(30L, 40L, 10L), expanded)
    }
}
