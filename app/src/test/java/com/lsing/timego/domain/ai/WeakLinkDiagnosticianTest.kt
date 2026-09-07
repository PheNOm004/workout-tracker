package com.lsing.timego.domain.ai

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeakLinkDiagnosticianTest {

    private val benchPress = Exercise(
        id = 1,
        name = "Barbell Bench Press",
        catalogueKey = "timego.seed.v1.barbell-bench-press",
        muscleGroups = listOf(MuscleGroup.CHEST.name, MuscleGroup.TRICEPS.name, MuscleGroup.FRONT_DELTS.name),
        isCustom = false,
        category = ExerciseCategory.STRENGTH.name,
        muscleWeights = mapOf(
            MuscleGroup.CHEST.name to 100,
            MuscleGroup.TRICEPS.name to 65,
            MuscleGroup.FRONT_DELTS.name to 50
        )
    )

    private val tricepPushdown = Exercise(
        id = 2,
        name = "Tricep Pushdown",
        catalogueKey = "timego.seed.v1.tricep-pushdown",
        muscleGroups = listOf(MuscleGroup.TRICEPS.name),
        isCustom = false,
        category = ExerciseCategory.STRENGTH.name,
        muscleWeights = mapOf(MuscleGroup.TRICEPS.name to 100)
    )

    private val dip = Exercise(
        id = 3,
        name = "Dip",
        catalogueKey = "timego.seed.v1.dip",
        muscleGroups = listOf(MuscleGroup.TRICEPS.name, MuscleGroup.CHEST.name),
        isCustom = false,
        category = ExerciseCategory.CALISTHENICS.name,
        muscleWeights = mapOf(MuscleGroup.TRICEPS to 70, MuscleGroup.CHEST to 50).mapKeys { it.key.name }
    )

    private val frontRaise = Exercise(
        id = 4,
        name = "Front Raise",
        catalogueKey = "timego.seed.v1.front-raise",
        muscleGroups = listOf(MuscleGroup.FRONT_DELTS.name),
        isCustom = false,
        category = ExerciseCategory.STRENGTH.name,
        muscleWeights = mapOf(MuscleGroup.FRONT_DELTS.name to 100)
    )

    @Test
    fun testDiagnoseBottleneck_identifiesTricepsWhenTricepVolumeIsZero() {
        val diagnostician = WeakLinkDiagnostician()
        val allExercises = listOf(benchPress, tricepPushdown, dip, frontRaise)

        // User logged lots of Front Delt work, but 0 Tricep work
        val recentSets = listOf(
            SetLog(id = 10, sessionId = 1, exerciseId = frontRaise.id, weightKg = 10.0, reps = 12, targetReps = 12, loggedAtEpochMillis = 1000L),
            SetLog(id = 11, sessionId = 1, exerciseId = frontRaise.id, weightKg = 10.0, reps = 12, targetReps = 12, loggedAtEpochMillis = 2000L)
        )

        val diagnosis = diagnostician.diagnoseBottleneck(benchPress, allExercises, recentSets)
        assertNotNull(diagnosis)
        assertEquals("Triceps", diagnosis!!.weakLinkMuscle)
        assertTrue("Should recommend Tricep Pushdown as strength accessory", diagnosis.strengthAccessories.any { it.name == "Tricep Pushdown" })
        assertTrue("Should recommend Dip as calisthenics accessory", diagnosis.calisthenicsAccessories.any { it.name == "Dip" })
    }

    @Test
    fun testDiagnoseBottleneck_returnsNullForPureIsolationExercise() {
        val diagnostician = WeakLinkDiagnostician()
        val bicepCurl = Exercise(
            id = 5,
            name = "Dumbbell Bicep Curl",
            catalogueKey = "timego.seed.v1.dumbbell-bicep-curl",
            muscleGroups = listOf(MuscleGroup.BICEPS.name),
            isCustom = false,
            category = ExerciseCategory.STRENGTH.name,
            muscleWeights = mapOf(MuscleGroup.BICEPS.name to 100)
        )

        val diagnosis = diagnostician.diagnoseBottleneck(bicepCurl, listOf(bicepCurl), emptyList())
        assertNull("Isolation exercise with no synergists should not produce a weak link", diagnosis)
    }
}
