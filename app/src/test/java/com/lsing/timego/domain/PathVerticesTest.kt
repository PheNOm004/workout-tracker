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
    fun `bare coordinate pairs right after moveto are implicit relative linetos`() {
        // This is the real bug: almost every traced muscle path looks like "M185,930 5 3 3 21..."
        // -- no letter between the initial moveto and the pairs that follow. Per SVG spec those
        // pairs are implicit linetos, relative because the source moveto was relative ("m"), even
        // though the baked data always starts with an absolute "M". Getting this wrong turns the
        // second point into another absolute jump instead of an offset from the first.
        val points = parsePathVertices("M185,930 5 3 3 21")
        assertEquals(listOf(185f to 930f, 190f to 933f, 193f to 954f), points)
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
