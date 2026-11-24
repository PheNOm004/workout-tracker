package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StimulatingRepsTest {
    private val bench = Exercise(id = 1, name = "Bench", muscleGroups = listOf("CHEST", "TRICEPS"), isCustom = false, muscleWeights = mapOf("TRICEPS" to 50))
    private fun set(reps: Int, rpe: Int?, warmup: Boolean = false, session: Long = 1) =
        SetLog(sessionId = session, exerciseId = 1, weightKg = 60.0, reps = reps, targetReps = reps, loggedAtEpochMillis = 1, isWarmup = warmup, rpe = rpe)

    @Test fun failureCountsFiveRepsAndEasySetsCountNone() {
        assertEquals(5.0, stimulatingReps(set(10, 10), bench)!!.reps, 1e-9)
        assertEquals(3.0, stimulatingReps(set(10, 8), bench)!!.reps, 1e-9)
        assertEquals(0.0, stimulatingReps(set(10, 5), bench)!!.reps, 1e-9)
    }

    @Test fun neverCountsMoreRepsThanWereDone() = assertEquals(2.0, stimulatingReps(set(2, 10), bench)!!.reps, 1e-9)

    @Test fun missingRpeFallsBackToThreeAndIsFlagged() {
        val result = stimulatingReps(set(10, null), bench)!!
        assertEquals(3.0, result.reps, 1e-9)
        assertTrue(result.estimated)
        assertEquals(1.0, stimulatingReps(set(1, null), bench)!!.reps, 1e-9)
    }

    @Test fun warmupsHoldsAndCardioAreExcluded() {
        assertNull(stimulatingReps(set(10, 9, warmup = true), bench))
        assertNull(stimulatingReps(set(10, 9), bench.copy(loggingType = LoggingType.HOLD.name)))
        assertNull(stimulatingReps(set(10, 9), bench.copy(category = ExerciseCategory.CARDIO.name)))
    }

    @Test fun unlistedPrimaryMuscleGetsFullCreditAndListedOnesScale() {
        val distribution = muscleGroupStimulatingReps(
            history = listOf(set(8, 9)),
            exercisesById = mapOf(1L to bench),
            sessionDateById = mapOf(1L to LocalDate.of(2026, 11, 1)),
            since = LocalDate.of(2026, 10, 1),
        )
        assertEquals(4.0, distribution.repsByGroup["CHEST"]!!, 1e-9)
        assertEquals(2.0, distribution.repsByGroup["TRICEPS"]!!, 1e-9)
        assertEquals(0.0, distribution.estimatedShare, 1e-9)
    }

    @Test fun estimatedShareIsTheFractionOfCountedSetsWithoutRpe() {
        val distribution = muscleGroupStimulatingReps(
            history = listOf(set(8, 9), set(8, null), set(8, null), set(8, 9, warmup = true)),
            exercisesById = mapOf(1L to bench),
            sessionDateById = mapOf(1L to LocalDate.of(2026, 11, 1)),
            since = LocalDate.of(2026, 10, 1),
        )
        assertEquals(2.0 / 3.0, distribution.estimatedShare, 1e-9)
    }

    @Test fun setsOutsideTheWindowAreIgnored() {
        val distribution = muscleGroupStimulatingReps(
            listOf(set(8, 9)), mapOf(1L to bench), mapOf(1L to LocalDate.of(2026, 1, 1)), LocalDate.of(2026, 10, 1),
        )
        assertTrue(distribution.repsByGroup.isEmpty())
    }
}
