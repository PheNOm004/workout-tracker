package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsymmetryTest {
    private var clock = 0L
    private fun set(session: Long, side: String?, kg: Double, reps: Int, warmup: Boolean = false) =
        SetLog(sessionId = session, exerciseId = 1, weightKg = kg, reps = reps, targetReps = reps, loggedAtEpochMillis = ++clock, isWarmup = warmup, side = side)

    /** Left lifts [leftKg] and right 20 kg for 8 reps in each of [sessions] sessions. */
    private fun sessions(sessions: Int, leftKg: Double) = (1L..sessions).flatMap {
        listOf(set(it, Side.LEFT, leftKg, 8), set(it, Side.RIGHT, 20.0, 8))
    }

    @Test fun percentIsBoundedAndSignedTowardTheStrongerSide() {
        assertEquals(20.0, asymmetryPercent(8.0, 10.0)!!, 1e-9)
        assertEquals(-20.0, asymmetryPercent(10.0, 8.0)!!, 1e-9)
        assertNull(asymmetryPercent(0.0, 0.0))
    }

    @Test fun pairsKthLeftWithKthRightAndIgnoresOrphans() {
        val sets = listOf(set(1, Side.LEFT, 16.0, 8), set(1, Side.LEFT, 16.0, 8), set(1, Side.RIGHT, 20.0, 8))
        val result = pairedSessionAsymmetries(sets)
        assertEquals(1, result.size)
        assertEquals(20.0, result.single().second, 1e-9)
        assertTrue(pairedSessionAsymmetries(listOf(set(1, Side.LEFT, 20.0, 8))).isEmpty())
    }

    @Test fun warmupsAndUnlabelledSetsAreExcluded() {
        val sets = listOf(set(1, Side.LEFT, 10.0, 8, warmup = true), set(1, Side.RIGHT, 20.0, 8), set(1, null, 5.0, 8))
        assertTrue(pairedSessionAsymmetries(sets).isEmpty())
    }

    @Test fun flagsOnlyAfterThreeAgreeingSessions() {
        assertNull(asymmetryInsight(sessions(2, 16.0)))
        val insight = asymmetryInsight(sessions(3, 17.5))
        assertNotNull(insight)
        assertEquals(Side.LEFT, insight!!.laggingSide)
        assertEquals(AsymmetryStatus.WATCH, insight.status)
    }

    @Test fun smallGapIsNotFlagged() = assertNull(asymmetryInsight(sessions(4, 19.0)))

    @Test fun largeGapIsADeficit() {
        val insight = asymmetryInsight(sessions(3, 14.0))!!
        assertEquals(AsymmetryStatus.DEFICIT, insight.status)
        assertTrue(insight.message.startsWith("Left lags by"))
    }

    @Test fun restWaitsForTheSecondSideAndSameSideTwiceStartsRest() {
        val first = stepSidePair(null, Side.LEFT)
        assertFalse(first.startRest)
        assertEquals(Side.LEFT, first.pending)
        val second = stepSidePair(first.pending, Side.RIGHT)
        assertTrue(second.startRest)
        assertNull(second.pending)
        assertTrue(stepSidePair(Side.LEFT, Side.LEFT).startRest)
    }
}
