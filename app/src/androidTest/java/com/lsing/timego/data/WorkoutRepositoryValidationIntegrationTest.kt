package com.lsing.timego.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryValidationIntegrationTest {
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
    fun invalidEntriesAreRejectedBeforeRoomReceivesThem() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.logSet(1, 2, Double.NaN, 8, 8) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.logCardioSet(1, 2, 0.0, null) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.logHoldSet(1, 2, 0, 60) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.logBodyMetric(LocalDate.of(2026, 9, 2), null, null, null) }
        }

        runBlocking {
            assertEquals(emptyList<SetLog>(), repository.setLogs.first())
            assertEquals(emptyList<BodyMetric>(), repository.bodyMetrics.first())
        }
    }
}
