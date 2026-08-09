package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PathVerticesTest {
    @Test
    fun `absolute moveto plus relative h,v,l traces the expected vertices`() {
        // A 10x10 square: start at (5,5), right 10, down 10, left+up back via l, close.
        val points = parsePathVertices("M5,5h10v10l-10,-10z")
        assertEquals(listOf(5f to 5f, 15f to 5f, 15f to 15f, 5f to 5f), points)
    }

    @Test
    fun `implicit repeated lineto pairs after a single l command all get consumed`() {
        val points = parsePathVertices("M0,0l1 1 2 2 3 3")
        assertEquals(listOf(0f to 0f, 1f to 1f, 3f to 3f, 6f to 6f), points)
    }

    @Test
    fun `shorthand negative numbers with no separating space tokenize correctly`() {
        val points = parsePathVertices("M0,0l3-1 2 1 1-2")
        assertEquals(listOf(0f to 0f, 3f to -1f, 5f to 0f, 6f to -2f), points)
    }

    @Test
    fun `repeated implicit h and v operands accumulate without a repeated command letter`() {
        val points = parsePathVertices("M0,0h5 5v3 3")
        assertEquals(listOf(0f to 0f, 5f to 0f, 10f to 0f, 10f to 3f, 10f to 6f), points)
    }
}
