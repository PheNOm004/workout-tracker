package com.lsing.timego.data.adaptive

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.data.WorkoutSession
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryShadowSnapshotIntegrationTest {
    private lateinit var database: TimeGoDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TimeGoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun closingAndDeletingSessionsPreserveSuggestionInputsForUnaffectedCanonicalSets() = runBlocking {
        val exercise = Exercise(
            name = "Synthetic exercise",
            catalogueKey = "timego.seed.v1.synthetic",
            muscleGroups = listOf("CHEST"),
            isCustom = false,
        )
        val exerciseId = database.exerciseDao().insert(exercise)
        val closingSessionId = database.sessionDao().insert(session(endEpochMillis = null))
        val deletingSessionId = database.sessionDao().insert(session(endEpochMillis = 1_200))
        val unaffectedSessionId = database.sessionDao().insert(session(endEpochMillis = 1_300))
        val closingSet = set(sessionId = closingSessionId, exerciseId = exerciseId, loggedAtEpochMillis = 100)
        val deletingSet = set(sessionId = deletingSessionId, exerciseId = exerciseId, loggedAtEpochMillis = 200)
        val unaffectedSet = set(sessionId = unaffectedSessionId, exerciseId = exerciseId, loggedAtEpochMillis = 300)
        database.setLogDao().insert(closingSet)
        database.setLogDao().insert(deletingSet)
        database.setLogDao().insert(unaffectedSet)

        repository.endSession(closingSessionId, endEpochMillis = 1_100)
        assertEquals(
            listOf(closingSet.copy(id = 1), deletingSet.copy(id = 2), unaffectedSet.copy(id = 3)),
            repository.setLogs.first(),
        )

        repository.deleteSession(deletingSessionId)
        assertEquals(
            listOf(closingSet.copy(id = 1), unaffectedSet.copy(id = 3)),
            repository.setLogs.first(),
        )
    }

    private fun session(endEpochMillis: Long?) = WorkoutSession(
        date = LocalDate.of(2026, 8, 20),
        routineId = null,
        startEpochMillis = 1_000,
        endEpochMillis = endEpochMillis,
    )

    private fun set(sessionId: Long, exerciseId: Long, loggedAtEpochMillis: Long) = SetLog(
        sessionId = sessionId,
        exerciseId = exerciseId,
        weightKg = 40.0,
        reps = 8,
        targetReps = 8,
        loggedAtEpochMillis = loggedAtEpochMillis,
    )
}
