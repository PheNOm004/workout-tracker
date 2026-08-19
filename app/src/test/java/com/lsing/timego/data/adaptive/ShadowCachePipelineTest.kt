package com.lsing.timego.data.adaptive

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.domain.adaptive.ShadowBasis
import com.lsing.timego.domain.adaptive.ShadowConfig
import com.lsing.timego.domain.adaptive.ShadowState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlin.math.ln1p

class ShadowCachePipelineTest {
    @Test
    fun `build maps session maxima replays every basis and encodes the derived states`() {
        val loaded = exercise(1, "timego.seed.v1.loaded", LoggingType.WEIGHT_REPS)
        val bodyweight = exercise(2, "timego.seed.v1.bodyweight", LoggingType.WEIGHT_REPS)
        val hold = exercise(3, "timego.seed.v1.hold", LoggingType.HOLD)
        val snapshot = ShadowSnapshot.from(
            sessions = listOf(session(1, 1_000), session(2, 2_000)),
            setLogs = listOf(
                set(1, 1, loaded.id, weightKg = 40.0, reps = 10),
                set(2, 1, loaded.id, weightKg = 60.0, reps = 8),
                set(3, 2, loaded.id, weightKg = 70.0, reps = 8),
                set(4, 1, bodyweight.id, weightKg = 0.0, reps = 12),
                set(5, 1, hold.id, weightKg = 0.0, reps = 0, holdSeconds = 30),
            ),
            exercises = listOf(loaded, bodyweight, hold),
        )
        val config = ShadowConfig(1, 1.0, 0.0, 0.1)
        val identity = ShadowCacheIdentity("model-v1", "metadata-v1")
        val pipeline = ShadowCachePipeline(config, identity) { key ->
            ShadowExerciseMetadata(
                demandVector = listOf(1.0),
                bodyweightSupported = key == bodyweight.catalogueKey,
            )
        }

        val write = pipeline.build(snapshot, completedAtEpochMillis = 9_000)

        val loadedBaseline = ln1p(60.0 * 8.0)
        val loadedChange = ln1p(70.0 * 8.0) - loadedBaseline
        val expectedStates = listOf(
            ShadowState(
                basis = ShadowBasis.LOAD_REPS,
                mean = listOf(loadedChange / 1.1),
                variance = listOf(1.0 - 1.0 / 1.1),
                observedAtEpochMillis = 2_000,
                personalBaselines = mapOf(loaded.catalogueKey!! to loadedBaseline),
            ),
            ShadowState(
                basis = ShadowBasis.REPS_ONLY,
                mean = listOf(0.0),
                variance = listOf(1.0),
                observedAtEpochMillis = 1_000,
                personalBaselines = mapOf(bodyweight.catalogueKey!! to ln1p(12.0)),
            ),
            ShadowState(
                basis = ShadowBasis.HOLD_SECONDS,
                mean = listOf(0.0),
                variance = listOf(1.0),
                observedAtEpochMillis = 1_000,
                personalBaselines = mapOf(hold.catalogueKey!! to ln1p(30.0)),
            ),
        )
        assertEquals(ShadowSourceFingerprint.from(snapshot), write.captured.sourceFingerprint)
        assertEquals(ShadowDerivedState.encode(expectedStates), write.statePayload)
        assertEquals(4, write.observationCount)
        assertEquals(1, write.exclusionCount)
    }

    private fun exercise(id: Long, key: String, loggingType: LoggingType) = Exercise(
        id = id,
        name = "Synthetic $id",
        catalogueKey = key,
        muscleGroups = listOf("CHEST"),
        isCustom = false,
        loggingType = loggingType.name,
    )

    private fun session(id: Long, end: Long) = WorkoutSession(
        id = id,
        date = LocalDate.of(2026, 8, 20),
        routineId = null,
        startEpochMillis = end - 100,
        endEpochMillis = end,
    )

    private fun set(
        id: Long,
        sessionId: Long,
        exerciseId: Long,
        weightKg: Double,
        reps: Int,
        holdSeconds: Int? = null,
    ) = SetLog(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        weightKg = weightKg,
        reps = reps,
        targetReps = reps,
        loggedAtEpochMillis = id * 100,
        holdSeconds = holdSeconds,
    )
}
