package com.lsing.timego.data.adaptive

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.domain.adaptive.ShadowConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryShadowCacheIntegrationTest {
    private val database: TimeGoDatabase = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        TimeGoDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val repository = WorkoutRepository(database)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletedCanonicalSourceDiscardsCapturedCacheAndRecordsOnlyAggregateStaleAudit() = runBlocking {
        val sessionId = insertCanonicalRow()
        val captured = repository.shadowSnapshot()
        val write = writeFor(captured, model = "contract-v1", metadata = "metadata-v1")

        assertEquals(ShadowCacheWriteDisposition.PERSISTED, repository.persistShadowCache(write).disposition)
        repository.deleteSession(sessionId)

        val stale = repository.persistShadowCache(write)

        assertEquals(ShadowCacheWriteDisposition.STALE_DISCARDED, stale.disposition)
        assertNull(database.shadowDao().snapshot())
        assertEquals(
            listOf(ShadowRebuildStatus.COMPLETED.name, ShadowRebuildStatus.STALE_DISCARDED.name),
            database.shadowDao().allAudit().map { it.rebuildStatus },
        )
        assertEquals(0, database.shadowDao().allAudit().last().sourceRowCount)
    }

    @Test
    fun modelOrMetadataChangeReplacesOnlyDerivedCacheAndAddsInvalidationAudit() = runBlocking {
        insertCanonicalRow()
        val captured = repository.shadowSnapshot()

        assertEquals(
            ShadowCacheWriteDisposition.PERSISTED,
            repository.persistShadowCache(writeFor(captured, model = "contract-v1", metadata = "metadata-v1")).disposition,
        )
        val changed = repository.persistShadowCache(writeFor(captured, model = "contract-v2", metadata = "metadata-v2"))

        assertEquals(ShadowCacheWriteDisposition.INVALIDATED, changed.disposition)
        assertEquals("contract-v2", database.shadowDao().snapshot()!!.modelContractHash)
        assertEquals("metadata-v2", database.shadowDao().snapshot()!!.metadataHash)
        assertEquals(ShadowRebuildStatus.COMPLETED.name, database.shadowDao().snapshot()!!.completionStatus)
        assertEquals(
            listOf(ShadowRebuildStatus.COMPLETED.name, ShadowRebuildStatus.INVALIDATED.name),
            database.shadowDao().allAudit().map { it.rebuildStatus },
        )
    }

    @Test
    fun realPipelineRebuildPersistsMappedReplayAndReturnsItThroughTheCheckedRead() = runBlocking {
        insertCanonicalRow()
        val identity = ShadowCacheIdentity("contract-v1", "metadata-v1")
        val pipeline = pipeline(identity)

        val decision = repository.rebuildShadowCache(pipeline, completedAtEpochMillis = 9_999)
        val usable = repository.usableShadowCache(identity)

        assertEquals(ShadowCacheWriteDisposition.PERSISTED, decision.disposition)
        assertNotNull(usable)
        assertEquals(1, usable!!.observationCount)
        assertEquals(0, usable.exclusionCount)
        assertEquals(ShadowRebuildStatus.COMPLETED.name, usable.completionStatus)
        assertEquals(ShadowRebuildStatus.COMPLETED.name, database.shadowDao().allAudit().single().rebuildStatus)
    }

    @Test
    fun checkedReadRejectsDeletedCanonicalSourceBeforeAnyLaterPersist() = runBlocking {
        val sessionId = insertCanonicalRow()
        val identity = ShadowCacheIdentity("contract-v1", "metadata-v1")
        repository.rebuildShadowCache(pipeline(identity), completedAtEpochMillis = 9_999)

        repository.deleteSession(sessionId)

        assertNull(repository.usableShadowCache(identity))
    }

    @Test
    fun checkedReadRejectsModelMetadataAndOrderingChangesBeforeAnyReplacementPersist() = runBlocking {
        insertCanonicalRow()
        val identity = ShadowCacheIdentity("contract-v1", "metadata-v1")
        repository.rebuildShadowCache(pipeline(identity), completedAtEpochMillis = 9_999)

        assertNull(repository.usableShadowCache(identity.copy(modelContractHash = "contract-v2")))
        assertNull(repository.usableShadowCache(identity.copy(metadataHash = "metadata-v2")))
        assertNull(repository.usableShadowCache(identity.copy(orderingPolicy = "different-order-v2")))
    }

    private suspend fun insertCanonicalRow(): Long {
        val exerciseId = database.exerciseDao().insert(
            Exercise(
                name = "Synthetic press",
                catalogueKey = "timego.seed.v1.synthetic-press",
                muscleGroups = listOf("CHEST"),
                isCustom = false,
            ),
        )
        val sessionId = database.sessionDao().insert(
            WorkoutSession(
                date = LocalDate.of(2026, 8, 20),
                routineId = null,
                startEpochMillis = 1_000,
                endEpochMillis = 2_000,
            ),
        )
        database.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = 40.0,
                reps = 8,
                targetReps = 8,
                loggedAtEpochMillis = 1_500,
            ),
        )
        return sessionId
    }

    private fun writeFor(snapshot: ShadowSnapshot, model: String, metadata: String) = ShadowCacheWrite(
        captured = ShadowCacheKey(
            sourceFingerprint = ShadowSourceFingerprint.from(snapshot),
            modelContractHash = model,
            metadataHash = metadata,
            orderingPolicy = "end-session-set-key-basis-v1",
        ),
        statePayload = "derived-state-v1",
        observationCount = 0,
        exclusionCount = 1,
        completedAtEpochMillis = 9_999,
    )

    private fun pipeline(identity: ShadowCacheIdentity) = ShadowCachePipeline(
        config = ShadowConfig(1, 1.0, 0.0, 0.1),
        identity = identity,
    ) { key ->
        if (key == "timego.seed.v1.synthetic-press") {
            ShadowExerciseMetadata(listOf(1.0), bodyweightSupported = false)
        } else {
            null
        }
    }
}
