package com.lsing.timego.ui.routines

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineExerciseOrderTest {
    private val list = listOf(1L, 2L, 3L, 4L)

    @Test fun movesDown() = assertEquals(listOf(2L, 3L, 1L, 4L), moveItem(list, 0, 2))
    @Test fun movesUp() = assertEquals(listOf(1L, 4L, 2L, 3L), moveItem(list, 3, 1))
    @Test fun sameIndexIsNoOp() = assertEquals(list, moveItem(list, 2, 2))
    @Test fun outOfRangeIsNoOp() = assertEquals(list, moveItem(list, 0, 9))
}
