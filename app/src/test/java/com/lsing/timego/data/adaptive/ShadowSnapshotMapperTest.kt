package com.lsing.timego.data.adaptive

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.domain.adaptive.ShadowBasis
import com.lsing.timego.domain.adaptive.ShadowObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import kotlin.math.ln1p

class ShadowSnapshotMapperTest {
    private val mapper = ShadowSnapshotMapper(coordinateCount = 1) { key ->
        if (key.startsWith("timego.seed.v1.")) {
            ShadowExerciseMetadata(
                demandVector = listOf(1.0),
                bodyweightSupported = key == "timego.seed.v1.bodyweight",
            )
        } else {
            null
        }
    }

    @Test
    fun `calisthenics uses its stored total load once`() {
        val mapping = mapper.map(
            snapshotOf(
                exercise = exercise(category = ExerciseCategory.CALISTHENICS.name),
                set = set(weightKg = 72.5, reps = 8),
            ),
        )

        assertEquals(1, mapping.observations.size)
        assertEquals(ShadowBasis.LOAD_REPS, mapping.observations.single().basis)
        assertEquals(ln1p(72.5 * 8), mapping.observations.single().observedWorkScore, 0.0)
        assertTrue(mapping.exclusions.isEmpty())
    }

    @Test
    fun `warmup is excluded without a shadow observation`() {
        val mapping = mapper.map(snapshotOf(set = set(isWarmup = true)))

        assertTrue(mapping.observations.isEmpty())
        assertEquals(ShadowSnapshotExclusionReason.WARMUP, mapping.exclusions.single().reason)
    }

    @Test
    fun `unkeyed custom exercise is explicitly excluded`() {
        val mapping = mapper.map(
            snapshotOf(exercise = exercise(catalogueKey = null, isCustom = true)),
        )

        assertTrue(mapping.observations.isEmpty())
        assertEquals(ShadowSnapshotExclusionReason.UNKEYED_CUSTOM_EXERCISE, mapping.exclusions.single().reason)
    }

    @Test
    fun `non positive session and set identities are excluded`() {
        val invalidSession = mapper.map(
            snapshotOf(session = closedSession(id = 0), set = set(sessionId = 0)),
        )
        val invalidSet = mapper.map(snapshotOf(set = set(id = 0)))

        assertTrue(invalidSession.observations.isEmpty())
        assertTrue(invalidSet.observations.isEmpty())
    }

    @Test
    fun `surrounding whitespace catalogue key is excluded instead of normalized`() {
        val mapping = mapper.map(
            snapshotOf(exercise = exercise(catalogueKey = " timego.seed.v1.squat ")),
        )

        assertTrue(mapping.observations.isEmpty())
    }

    @Test
    fun `duration only cardio is explicitly excluded`() {
        val mapping = mapper.map(
            snapshotOf(
                exercise = exercise(
                    category = ExerciseCategory.CARDIO.name,
                    loggingType = LoggingType.DURATION_DISTANCE.name,
                ),
                set = set(weightKg = 0.0, reps = 0, durationMinutes = 30.0),
            ),
        )

        assertTrue(mapping.observations.isEmpty())
        assertEquals(ShadowSnapshotExclusionReason.DURATION_ONLY_CARDIO, mapping.exclusions.single().reason)
    }

    @Test
    fun `weighted reps reps only and holds keep separate measurement bases`() {
        val session = closedSession()
        val weighted = exercise(id = 1, catalogueKey = "timego.seed.v1.weighted")
        val bodyweight = exercise(id = 2, catalogueKey = "timego.seed.v1.bodyweight")
        val hold = exercise(id = 3, catalogueKey = "timego.seed.v1.hold", loggingType = LoggingType.HOLD.name)
        val snapshot = ShadowSnapshot.from(
            sessions = listOf(session),
            setLogs = listOf(
                set(id = 1, exerciseId = weighted.id, weightKg = 20.0, reps = 5),
                set(id = 2, exerciseId = bodyweight.id, weightKg = 0.0, reps = 12),
                set(id = 3, exerciseId = hold.id, weightKg = 0.0, reps = 0, holdSeconds = 30),
            ),
            exercises = listOf(weighted, bodyweight, hold),
        )

        val observations = mapper.map(snapshot).observations.associateBy { it.catalogueKey }

        assertEquals(ShadowBasis.LOAD_REPS, observations.getValue(weighted.catalogueKey!!).basis)
        assertEquals(ShadowBasis.REPS_ONLY, observations.getValue(bodyweight.catalogueKey!!).basis)
        assertEquals(ShadowBasis.HOLD_SECONDS, observations.getValue(hold.catalogueKey!!).basis)
    }

    @Test
    fun `one maximum completed work point is selected per exercise basis and closed session`() {
        val exercise = exercise(catalogueKey = "timego.seed.v1.bodyweight")
        val snapshot = ShadowSnapshot.from(
            sessions = listOf(closedSession()),
            setLogs = listOf(
                set(id = 1, exerciseId = exercise.id, weightKg = 70.0, reps = 5, loggedAtEpochMillis = 100),
                set(id = 2, exerciseId = exercise.id, weightKg = 60.0, reps = 8, loggedAtEpochMillis = 200),
                set(id = 3, exerciseId = exercise.id, weightKg = 0.0, reps = 6, loggedAtEpochMillis = 300),
                set(id = 4, exerciseId = exercise.id, weightKg = 0.0, reps = 12, loggedAtEpochMillis = 400),
            ),
            exercises = listOf(exercise),
        )

        val mapping = mapper.map(snapshot)

        assertEquals(listOf(2L, 4L), mapping.observations.map { it.setId })
        assertEquals(
            listOf(ShadowBasis.LOAD_REPS, ShadowBasis.REPS_ONLY),
            mapping.observations.map { it.basis },
        )
        assertEquals(
            listOf(1L, 3L),
            mapping.exclusions
                .filter { it.reason == ShadowSnapshotExclusionReason.SESSION_SUMMARY_NOT_SELECTED }
                .map { it.setId },
        )
    }

    @Test
    fun `equal work selects the latest set time then highest set id deterministically`() {
        val exercise = exercise()
        val snapshot = ShadowSnapshot.from(
            sessions = listOf(closedSession()),
            setLogs = listOf(
                set(id = 1, exerciseId = exercise.id, weightKg = 40.0, reps = 5, loggedAtEpochMillis = 300),
                set(id = 3, exerciseId = exercise.id, weightKg = 20.0, reps = 10, loggedAtEpochMillis = 400),
                set(id = 2, exerciseId = exercise.id, weightKg = 25.0, reps = 8, loggedAtEpochMillis = 400),
            ),
            exercises = listOf(exercise),
        )

        val mapping = mapper.map(snapshot)

        assertEquals(3L, mapping.observations.single().setId)
    }

    @Test
    fun `zero load requires reviewed bodyweight metadata before it can become reps only evidence`() {
        val mapping = mapper.map(
            snapshotOf(
                exercise = exercise(catalogueKey = "timego.seed.v1.ordinary-strength"),
                set = set(weightKg = 0.0, reps = 12),
            ),
        )

        assertTrue(mapping.observations.isEmpty())
        assertEquals(ShadowSnapshotExclusionReason.INVALID_WORK, mapping.exclusions.single().reason)
    }

    @Test
    fun `reviewed bodyweight metadata permits zero load reps only evidence`() {
        val mapping = mapper.map(
            snapshotOf(
                exercise = exercise(catalogueKey = "timego.seed.v1.bodyweight"),
                set = set(weightKg = 0.0, reps = 12),
            ),
        )

        assertEquals(ShadowBasis.REPS_ONLY, mapping.observations.single().basis)
        assertEquals(ln1p(12.0), mapping.observations.single().observedWorkScore, 0.0)
    }

    @Test
    fun `negative and non finite loads are excluded even for reviewed bodyweight exercises`() {
        val exercise = exercise(catalogueKey = "timego.seed.v1.bodyweight")
        val snapshot = ShadowSnapshot.from(
            sessions = listOf(closedSession()),
            setLogs = listOf(
                set(id = 1, exerciseId = exercise.id, weightKg = -1.0, reps = 8),
                set(id = 2, exerciseId = exercise.id, weightKg = Double.NaN, reps = 8),
                set(id = 3, exerciseId = exercise.id, weightKg = Double.POSITIVE_INFINITY, reps = 8),
            ),
            exercises = listOf(exercise),
        )

        val mapping = mapper.map(snapshot)

        assertTrue(mapping.observations.isEmpty())
        assertEquals(3, mapping.exclusions.count { it.reason == ShadowSnapshotExclusionReason.INVALID_WORK })
    }

    @Test
    fun `snapshot uses end session set time then set id as deterministic ties`() {
        val firstSession = closedSession(id = 2, endEpochMillis = 2_000)
        val laterSession = closedSession(id = 3, endEpochMillis = 3_000)
        val exercise = exercise()
        val snapshot = ShadowSnapshot.from(
            sessions = listOf(laterSession, firstSession),
            setLogs = listOf(
                set(id = 30, sessionId = 3, exerciseId = exercise.id, loggedAtEpochMillis = 1),
                set(id = 21, sessionId = 2, exerciseId = exercise.id, loggedAtEpochMillis = 200),
                set(id = 20, sessionId = 2, exerciseId = exercise.id, loggedAtEpochMillis = 100),
            ),
            exercises = listOf(exercise),
        )

        assertEquals(listOf(20L, 21L, 30L), snapshot.rows.map { it.setLog.id })
    }

    @Test
    fun `mapping leaves current rule based suggestion input rows unchanged`() {
        val sourceSet = set(weightKg = 40.0, reps = 8)
        val sourceRows = snapshotOf(set = sourceSet).rows

        mapper.map(ShadowSnapshot.fromRows(sourceRows))

        assertEquals(sourceSet, sourceRows.single().setLog)
    }

    @Test
    fun `snapshot and mapping outputs are defensive unmodifiable copies`() {
        val sourceRows = mutableListOf(snapshotOf().rows.single())
        val snapshot = ShadowSnapshot.fromRows(sourceRows)
        sourceRows.clear()

        assertEquals(1, snapshot.rows.size)
        assertUnmodifiable { (snapshot.rows as MutableList<ShadowSnapshotRow>).clear() }

        val mapping = mapper.map(snapshot)
        assertUnmodifiable { (mapping.observations as MutableList<*>).clear() }
        assertUnmodifiable { (mapping.exclusions as MutableList<*>).clear() }
    }

    @Test
    fun `direct mapping construction copies source collections and exposes immutable outputs`() {
        val sourceObservations = mutableListOf(
            ShadowObservation(
                catalogueKey = "timego.seed.v1.squat",
                sessionId = 1,
                setId = 1,
                endedAtEpochMillis = 1_000,
                demandVector = listOf(1.0),
                basis = ShadowBasis.LOAD_REPS,
                observedWorkScore = 1.0,
            ),
        )
        val sourceExclusions = mutableListOf(
            ShadowSnapshotExclusion(1, 2, ShadowSnapshotExclusionReason.WARMUP),
        )

        val mapping = ShadowSnapshotMapping(sourceObservations, sourceExclusions)
        sourceObservations.clear()
        sourceExclusions.clear()

        assertEquals(1, mapping.observations.size)
        assertEquals(1, mapping.exclusions.size)
        assertUnmodifiable { (mapping.observations as MutableList<*>).clear() }
        assertUnmodifiable { (mapping.exclusions as MutableList<*>).clear() }
    }

    private fun assertUnmodifiable(action: () -> Unit) {
        try {
            action()
            fail("Expected an unmodifiable collection")
        } catch (_: UnsupportedOperationException) {
        }
    }

    private fun snapshotOf(
        exercise: Exercise = exercise(),
        set: SetLog = set(exerciseId = exercise.id),
        session: WorkoutSession = closedSession(id = set.sessionId),
    ): ShadowSnapshot = ShadowSnapshot.from(
        sessions = listOf(session),
        setLogs = listOf(set),
        exercises = listOf(exercise),
    )

    private fun exercise(
        id: Long = 1,
        catalogueKey: String? = "timego.seed.v1.squat",
        isCustom: Boolean = false,
        category: String = ExerciseCategory.STRENGTH.name,
        loggingType: String = LoggingType.WEIGHT_REPS.name,
    ) = Exercise(
        id = id,
        name = "Synthetic exercise $id",
        catalogueKey = catalogueKey,
        muscleGroups = listOf("CHEST"),
        isCustom = isCustom,
        category = category,
        loggingType = loggingType,
    )

    private fun closedSession(id: Long = 1, endEpochMillis: Long = 1_000) = WorkoutSession(
        id = id,
        date = LocalDate.of(2026, 8, 20),
        routineId = null,
        startEpochMillis = endEpochMillis - 100,
        endEpochMillis = endEpochMillis,
    )

    private fun set(
        id: Long = 1,
        sessionId: Long = 1,
        exerciseId: Long = 1,
        weightKg: Double = 40.0,
        reps: Int = 8,
        loggedAtEpochMillis: Long = 500,
        holdSeconds: Int? = null,
        durationMinutes: Double? = null,
        isWarmup: Boolean = false,
    ) = SetLog(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        weightKg = weightKg,
        reps = reps,
        targetReps = reps,
        loggedAtEpochMillis = loggedAtEpochMillis,
        holdSeconds = holdSeconds,
        durationMinutes = durationMinutes,
        isWarmup = isWarmup,
    )
}
