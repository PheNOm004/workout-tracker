package com.lsing.timego.data.adaptive

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.MIGRATION_11_12
import com.lsing.timego.data.MIGRATION_12_13
import com.lsing.timego.data.MIGRATION_13_14
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TimeGoDatabaseMigration14Test {
    @Test
    fun migratesSchema11Through12And13To14WhilePreservingCanonicalRows() {
        migrateAndAssertCanonicalRows(
            sourceVersion = 11,
            expectedCatalogueKey = null,
            expectedTargetProvenance = "UNKNOWN",
        )
    }

    @Test
    fun migratesSchema12Through13To14WhilePreservingCanonicalRows() {
        migrateAndAssertCanonicalRows(
            sourceVersion = 12,
            expectedCatalogueKey = null,
            expectedTargetProvenance = "OVERLOAD_SUGGESTION",
        )
    }

    @Test
    fun migratesSchema13WithoutChangingCanonicalRowsAndCreatesEmptyShadowTables() {
        migrateAndAssertCanonicalRows(
            sourceVersion = 13,
            expectedCatalogueKey = "timego.seed.v1.synthetic-press",
            expectedTargetProvenance = "UNKNOWN",
        )
    }

    private fun migrateAndAssertCanonicalRows(
        sourceVersion: Int,
        expectedCatalogueKey: String?,
        expectedTargetProvenance: String,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DATABASE)
        createLegacyDatabase(context, sourceVersion)

        val migrated = Room.databaseBuilder(context, TimeGoDatabase::class.java, TEST_DATABASE)
            .addMigrations(
                *when (sourceVersion) {
                    11 -> arrayOf(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    12 -> arrayOf(MIGRATION_12_13, MIGRATION_13_14)
                    13 -> arrayOf(MIGRATION_13_14)
                    else -> error("Unsupported legacy test schema $sourceVersion")
                },
            )
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                assertEquals(
                    Exercise(
                        id = 71,
                        name = "Synthetic press",
                        catalogueKey = expectedCatalogueKey,
                        muscleGroups = listOf("CHEST"),
                        isCustom = false,
                    ),
                    migrated.exerciseDao().getById(71),
                )
                assertEquals(
                    listOf(
                        WorkoutSession(
                            id = 72,
                            date = LocalDate.ofEpochDay(20500),
                            routineId = null,
                            startEpochMillis = 1_000,
                            endEpochMillis = 2_000,
                        ),
                    ),
                    migrated.sessionDao().allForShadowSnapshot(),
                )
                assertEquals(
                    listOf(
                        SetLog(
                            id = 73,
                            sessionId = 72,
                            exerciseId = 71,
                            weightKg = 40.0,
                            reps = 8,
                            targetReps = 8,
                            loggedAtEpochMillis = 1_500,
                            targetProvenance = expectedTargetProvenance,
                        ),
                    ),
                    migrated.setLogDao().getAll(),
                )
                assertEquals(0, migrated.shadowDao().allAudit().size)
                assertEquals(null, migrated.shadowDao().snapshot())
            }
        } finally {
            migrated.close()
            context.deleteDatabase(TEST_DATABASE)
        }
    }

    /** DDL and Room identities are copied from the committed exported schemas 11, 12, and 13. */
    private fun createLegacyDatabase(context: Context, version: Int) {
        context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null).use { database ->
            val catalogueColumn = if (version >= 13) ", catalogueKey TEXT" else ""
            val provenanceColumn = if (version >= 12) ", targetProvenance TEXT NOT NULL" else ""
            database.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL$catalogueColumn, muscleGroups TEXT NOT NULL, isCustom INTEGER NOT NULL, category TEXT NOT NULL, loggingType TEXT NOT NULL, muscleWeights TEXT NOT NULL)")
            database.execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, routineId INTEGER, startEpochMillis INTEGER NOT NULL, endEpochMillis INTEGER)")
            database.execSQL("CREATE TABLE set_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, weightKg REAL NOT NULL, reps INTEGER NOT NULL, targetReps INTEGER NOT NULL, loggedAtEpochMillis INTEGER NOT NULL, durationMinutes REAL, distanceKm REAL, holdSeconds INTEGER, targetHoldSeconds INTEGER, isWarmup INTEGER NOT NULL, addedWeightKg REAL, rpe INTEGER$provenanceColumn)")
            database.execSQL("CREATE INDEX index_set_logs_sessionId ON set_logs (sessionId)")
            database.execSQL("CREATE INDEX index_set_logs_exerciseId ON set_logs (exerciseId)")
            database.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, daysOfWeek TEXT NOT NULL)")
            database.execSQL("CREATE TABLE routine_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderIndex INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX index_routine_exercises_routineId ON routine_exercises (routineId)")
            database.execSQL("CREATE TABLE body_metrics (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, weightKg REAL, waistCm REAL, heightCm REAL)")
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            val identity = when (version) {
                11 -> "826a82ab622903ff171d4d17ba08cb9a"
                12 -> "16c832549311781953140266ff8ee44f"
                13 -> "6f433c822ab6c4fc099ca17cddbcfbac"
                else -> error("Unsupported legacy test schema $version")
            }
            database.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '$identity')")
            database.execSQL("PRAGMA user_version = $version")
            if (version >= 13) {
                database.execSQL("INSERT INTO exercises (id, name, catalogueKey, muscleGroups, isCustom, category, loggingType, muscleWeights) VALUES (71, 'Synthetic press', 'timego.seed.v1.synthetic-press', 'CHEST', 0, 'STRENGTH', 'WEIGHT_REPS', '')")
            } else {
                database.execSQL("INSERT INTO exercises (id, name, muscleGroups, isCustom, category, loggingType, muscleWeights) VALUES (71, 'Synthetic press', 'CHEST', 0, 'STRENGTH', 'WEIGHT_REPS', '')")
            }
            database.execSQL("INSERT INTO workout_sessions (id, date, routineId, startEpochMillis, endEpochMillis) VALUES (72, 20500, NULL, 1000, 2000)")
            if (version >= 12) {
                val provenance = if (version == 12) "OVERLOAD_SUGGESTION" else "UNKNOWN"
                database.execSQL("INSERT INTO set_logs (id, sessionId, exerciseId, weightKg, reps, targetReps, loggedAtEpochMillis, durationMinutes, distanceKm, holdSeconds, targetHoldSeconds, isWarmup, addedWeightKg, rpe, targetProvenance) VALUES (73, 72, 71, 40.0, 8, 8, 1500, NULL, NULL, NULL, NULL, 0, NULL, NULL, '$provenance')")
            } else {
                database.execSQL("INSERT INTO set_logs (id, sessionId, exerciseId, weightKg, reps, targetReps, loggedAtEpochMillis, durationMinutes, distanceKm, holdSeconds, targetHoldSeconds, isWarmup, addedWeightKg, rpe) VALUES (73, 72, 71, 40.0, 8, 8, 1500, NULL, NULL, NULL, NULL, 0, NULL, NULL)")
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "timego-migration-14-test"
    }
}
