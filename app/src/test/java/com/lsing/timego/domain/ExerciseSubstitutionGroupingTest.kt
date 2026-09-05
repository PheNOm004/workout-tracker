package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures copied verbatim from SeedExercises.kt's tag values, not re-derived, so a mismatch
 *  here means the real seed data drifted, not a fixture typo. */
class ExerciseSubstitutionGroupingTest {
    private fun exercise(
        id: Long,
        name: String,
        vararg groups: MuscleGroup,
        weights: Map<MuscleGroup, Int> = emptyMap(),
        loggingType: LoggingType = LoggingType.WEIGHT_REPS,
    ) = Exercise(
        id = id,
        name = name,
        muscleGroups = groups.map { it.name },
        isCustom = false,
        loggingType = loggingType.name,
        muscleWeights = weights.mapKeys { it.key.name },
    )

    private val pushUp = exercise(1, "Push-Up", MuscleGroup.CHEST, MuscleGroup.TRICEPS, weights = mapOf(MuscleGroup.TRICEPS to 60))
    private val declinePushUp = exercise(2, "Decline Push-Up", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, weights = mapOf(MuscleGroup.FRONT_DELTS to 50))
    private val handstandPushUp = exercise(3, "Handstand Push-Up", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS, weights = mapOf(MuscleGroup.SIDE_DELTS to 55, MuscleGroup.TRICEPS to 60))
    private val diamondPushUp = exercise(4, "Diamond Push-Up", MuscleGroup.TRICEPS, MuscleGroup.CHEST, weights = mapOf(MuscleGroup.CHEST to 55))
    private val bicepCurl = exercise(5, "Ring Bicep Curl", MuscleGroup.BICEPS, MuscleGroup.FOREARMS, weights = mapOf(MuscleGroup.FOREARMS to 30))
    private val barbellRow = exercise(6, "Barbell Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.TRAPS, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 70, MuscleGroup.TRAPS to 30, MuscleGroup.BICEPS to 35))
    private val pullUp = exercise(7, "Pull-Up", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 65, MuscleGroup.BICEPS to 40))
    private val catalogue = listOf(pushUp, declinePushUp, handstandPushUp, diamondPushUp, bicepCurl, barbellRow, pullUp)

    @Test
    fun `push angle is derived from existing muscle weights -- zero for chest-dominant, ninety for shoulder-only`() {
        assertEquals(0.0, movementAngle(pushUp)!!.second, 0.001)
        assertEquals(30.0, movementAngle(declinePushUp)!!.second, 0.001)
        assertEquals(90.0, movementAngle(handstandPushUp)!!.second, 0.001)
    }

    @Test
    fun `pull angle is keyword-bucketed, not derived -- rows and pull-ups tag near-identical muscles`() {
        assertEquals(0.0, movementAngle(barbellRow)!!.second, 0.001)
        assertEquals(90.0, movementAngle(pullUp)!!.second, 0.001)
    }

    @Test
    fun `push-up and diamond push-up are highly related -- same angle, high muscle overlap`() {
        val candidates = substitutionCandidatesFor(pushUp, catalogue)
        val diamond = candidates.first { it.exercise == diamondPushUp }
        assertTrue("expected strong relatedness, got ${diamond.relatedness}", diamond.relatedness > 0.7)
    }

    @Test
    fun `decline push-up and handstand push-up are weakly related -- same family, partway apart in angle`() {
        // The motivating example from the brainstorm conversation. Under the old hard-category
        // plane gate these never related at all; under the continuous-angle model they get a low
        // but nonzero score reflecting "same family, most of the way across the incline range".
        val candidates = substitutionCandidatesFor(declinePushUp, catalogue)
        val handstand = candidates.firstOrNull { it.exercise == handstandPushUp }
        assertTrue("expected decline push-up to weakly relate to handstand push-up", handstand != null)
        assertTrue("expected a low score, got ${handstand!!.relatedness}", handstand.relatedness < 0.3)
    }

    @Test
    fun `push-up and handstand push-up do not relate -- angle distance is the full 90 degrees`() {
        // Opposite ends of the same family's angle range cancel relatedness out entirely,
        // regardless of any muscle overlap (both tag TRICEPS here).
        assertTrue(substitutionCandidatesFor(pushUp, catalogue).none { it.exercise == handstandPushUp })
    }

    @Test
    fun `push never relates to pull, and vice versa`() {
        assertTrue(substitutionCandidatesFor(pushUp, catalogue).none { it.exercise == barbellRow || it.exercise == pullUp })
        assertTrue(substitutionCandidatesFor(barbellRow, catalogue).none { it.exercise == pushUp })
    }

    @Test
    fun `an exercise with no recognisable family keyword abstains entirely`() {
        val mysteryExercise = exercise(8, "Svend Press", MuscleGroup.CHEST)
        assertEquals(MovementFamily.OTHER, deriveFamily(mysteryExercise.name))
        assertTrue(substitutionCandidatesFor(mysteryExercise, catalogue).isEmpty())
    }

    @Test
    fun `hold-type and weight-reps exercises never relate even in the same family`() {
        val handstandHold = exercise(
            9,
            "Freestanding Handstand Hold",
            MuscleGroup.FRONT_DELTS,
            MuscleGroup.SIDE_DELTS,
            weights = mapOf(MuscleGroup.SIDE_DELTS to 55),
            loggingType = LoggingType.HOLD,
        )
        assertEquals(MovementFamily.PUSH, deriveFamily(handstandHold.name))
        assertTrue(substitutionCandidatesFor(handstandPushUp, catalogue + handstandHold).none { it.exercise == handstandHold })
    }
}
