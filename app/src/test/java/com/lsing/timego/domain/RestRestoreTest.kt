package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestRestoreTest {
    @Test
    fun onlyAnUnfinishedRestComesBack() {
        assertEquals(RestTimerState(1_000, 120), restorableRest(1_000, 120, 61_000))
        assertNull(restorableRest(1_000, 120, 121_000))
        assertNull(restorableRest(0, 0, 61_000))
        assertNull(restorableRest(90_000, 120, 61_000))
    }
}
