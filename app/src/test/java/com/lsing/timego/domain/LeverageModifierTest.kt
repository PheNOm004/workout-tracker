package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeverageModifierTest {
    private val lever = Exercise(id = 1, name = "Front Lever", muscleGroups = listOf("LATS"), isCustom = false, category = ExerciseCategory.CALISTHENICS.name, loggingType = LoggingType.HOLD.name)
    private val days = mapOf(1L to LocalDate.of(2026, 11, 1), 2L to LocalDate.of(2026, 11, 8))

    private fun hold(session: Long, seconds: Int, kind: String? = null, value: String? = null, warmup: Boolean = false) =
        SetLog(sessionId = session, exerciseId = 1, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 1, holdSeconds = seconds, isWarmup = warmup, modifierKind = kind, modifierValue = value)

    @Test fun assistedHoldNeverReplacesTheCleanRecord() {
        val history = listOf(hold(1, 10), hold(2, 25, LeverageKind.BAND, "MEDIUM"))
        val record = personalRecords(history, days, mapOf(1L to lever)).single { it.type == PrType.LONGEST_HOLD }
        assertEquals(10.0, record.value, 1e-9)
    }

    @Test fun onlyAssistedHoldsMeansNoCleanRecord() {
        val history = listOf(hold(1, 20, LeverageKind.BAND, "LIGHT"))
        assertNull(personalRecords(history, days, mapOf(1L to lever)).firstOrNull { it.type == PrType.LONGEST_HOLD })
    }

    @Test fun mainCurveUsesCleanHoldsOnly() {
        val history = listOf(hold(1, 10), hold(1, 30, LeverageKind.ASSIST_KG, "10"), hold(2, 12))
        val curve = exerciseProgressCurve(lever, history, days)
        assertEquals(listOf(10.0, 12.0), curve.points.map { it.second })
    }

    @Test fun assistedBestsGroupByModifierLongestFirstAndSkipWarmups() {
        val history = listOf(
            hold(1, 15, LeverageKind.BAND, "MEDIUM"), hold(2, 20, LeverageKind.BAND, "MEDIUM"),
            hold(2, 12, LeverageKind.BAND, "HEAVY"), hold(2, 40, LeverageKind.BAND, "HEAVY", warmup = true), hold(1, 8),
        )
        val bests = assistedHoldBests(lever, history)
        assertEquals(listOf("Medium band" to 20, "Heavy band" to 12), bests.map { it.label to it.seconds })
    }

    @Test fun labelsKnownPresetsAndFallsBackForUnknownOnes() {
        assertEquals("Light band", modifierLabel(LeverageKind.BAND, "LIGHT"))
        assertEquals("BAND TINY", modifierLabel("BAND", "TINY"))
        assertNull(modifierLabel(null, null))
    }
}
