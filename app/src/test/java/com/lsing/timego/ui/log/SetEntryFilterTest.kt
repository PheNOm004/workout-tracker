package com.lsing.timego.ui.log

import org.junit.Assert.assertEquals
import org.junit.Test

class SetEntryFilterTest {
    @Test
    fun repsKeepOnlyDigitsAndCapLength() {
        assertEquals("12", filterWholeNumber("1a2"))
        assertEquals("", filterWholeNumber("barbell bench"))
        assertEquals("123", filterWholeNumber("12345"))
        assertEquals("8", filterWholeNumber("8.5"[0].toString()))
    }

    @Test
    fun weightKeepsOnePointAndTwoDecimals() {
        assertEquals("62.5", filterDecimal("62.5"))
        assertEquals("62.5", filterDecimal("62,5"))
        assertEquals("62.55", filterDecimal("62.555"))
        assertEquals("1.2", filterDecimal("1.2.3").substring(0, 3))
        assertEquals("", filterDecimal("abc"))
        assertEquals("100", filterDecimal("100"))
    }
}
