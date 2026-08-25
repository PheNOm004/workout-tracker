package com.lsing.timego.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumericInputTest {
    @Test
    fun finiteDouble_rejectsNaNAndInfinity() {
        assertNull("NaN".toFiniteDoubleOrNull())
        assertNull("Infinity".toFiniteDoubleOrNull())
        assertEquals(-2.5, "-2.5".toFiniteDoubleOrNull()!!, 0.0)
    }

    @Test
    fun positiveNumbers_rejectZeroAndNegativeValues() {
        assertNull("0".toPositiveFiniteDoubleOrNull())
        assertNull("-1".toPositiveFiniteDoubleOrNull())
        assertNull("0".toPositiveIntOrNull())
        assertNull("-1".toPositiveIntOrNull())
        assertEquals(2.5, "2.5".toPositiveFiniteDoubleOrNull()!!, 0.0)
        assertEquals(3, "3".toPositiveIntOrNull())
    }
}
