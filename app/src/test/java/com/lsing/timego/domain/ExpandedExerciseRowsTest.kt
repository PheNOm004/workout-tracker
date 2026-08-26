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
    fun `opening a second row closes the first -- only one panel stays open at a time`() {
        val expanded = toggleExpandedExerciseIds(listOf(10L), 20L)

        assertEquals(listOf(20L), expanded)
    }

    @Test
    fun `collapses the tapped expanded row`() {
        val expanded = toggleExpandedExerciseIds(listOf(20L), 20L)

        assertEquals(emptyList<Long>(), expanded)
    }

    @Test
    fun `reopening a collapsed row makes it the open row`() {
        val expanded = toggleExpandedExerciseIds(listOf(20L), 10L)

        assertEquals(listOf(10L), expanded)
    }

    @Test
    fun `an explicit maxExpanded still keeps the newest N rows -- the helper stays generic`() {
        val expanded = toggleExpandedExerciseIds(listOf(10L, 20L, 30L), 40L, maxExpanded = 3)

        assertEquals(listOf(20L, 30L, 40L), expanded)
    }

    @Test
    fun `an explicit maxExpanded still collapses only the tapped row among several open`() {
        val expanded = toggleExpandedExerciseIds(listOf(10L, 20L, 30L), 20L, maxExpanded = 3)

        assertEquals(listOf(10L, 30L), expanded)
    }
}
