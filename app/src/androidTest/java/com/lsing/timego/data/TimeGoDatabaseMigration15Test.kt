package com.lsing.timego.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeGoDatabaseMigration15Test {
    @Test
    fun migration14To15PreservesRowsAndAddsChronologicalSetIndex() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DATABASE)
        createSchema14Database(context)

        val migrated = Room.databaseBuilder(context, TimeGoDatabase::class.java, TEST_DATABASE)
            .addMigrations(MIGRATION_14_15)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                assertEquals(1, migrated.setLogDao().getAll().size)
            }
            val indexes = migrated.openHelper.readableDatabase
                .query("PRAGMA index_list(`set_logs`)")
                .use { cursor ->
                    buildSet {
                        val nameColumn = cursor.getColumnIndexOrThrow("name")
                        while (cursor.moveToNext()) add(cursor.getString(nameColumn))
                    }
                }
            assertTrue("Expected timestamp index after migration", "index_set_logs_loggedAtEpochMillis" in indexes)
        } finally {
            migrated.close()
            context.deleteDatabase(TEST_DATABASE)
        }
    }

    private fun createSchema14Database(context: Context) {
        context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, catalogueKey TEXT, muscleGroups TEXT NOT NULL, isCustom INTEGER NOT NULL, category TEXT NOT NULL, loggingType TEXT NOT NULL, muscleWeights TEXT NOT NULL)")
            database.execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, routineId INTEGER, startEpochMillis INTEGER NOT NULL, endEpochMillis INTEGER)")
            database.execSQL("CREATE TABLE set_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, weightKg REAL NOT NULL, reps INTEGER NOT NULL, targetReps INTEGER NOT NULL, loggedAtEpochMillis INTEGER NOT NULL, durationMinutes REAL, distanceKm REAL, holdSeconds INTEGER, targetHoldSeconds INTEGER, isWarmup INTEGER NOT NULL, addedWeightKg REAL, rpe INTEGER, targetProvenance TEXT NOT NULL)")
            database.execSQL("CREATE INDEX index_set_logs_sessionId ON set_logs (sessionId)")
            database.execSQL("CREATE INDEX index_set_logs_exerciseId ON set_logs (exerciseId)")
            database.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, daysOfWeek TEXT NOT NULL)")
            database.execSQL("CREATE TABLE routine_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderIndex INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX index_routine_exercises_routineId ON routine_exercises (routineId)")
            database.execSQL("CREATE TABLE body_metrics (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, weightKg REAL, waistCm REAL, heightCm REAL)")
            database.execSQL("CREATE TABLE shadow_snapshots (cacheKey TEXT NOT NULL, sourceFingerprint TEXT NOT NULL, modelContractHash TEXT NOT NULL, metadataHash TEXT NOT NULL, orderingPolicy TEXT NOT NULL, statePayload TEXT NOT NULL, sourceRowCount INTEGER NOT NULL, observationCount INTEGER NOT NULL, exclusionCount INTEGER NOT NULL, completionStatus TEXT NOT NULL, completedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(cacheKey))")
            database.execSQL("CREATE TABLE shadow_audit (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sourceFingerprint TEXT NOT NULL, modelContractHash TEXT NOT NULL, metadataHash TEXT NOT NULL, orderingPolicy TEXT NOT NULL, sourceRowCount INTEGER NOT NULL, observationCount INTEGER NOT NULL, exclusionCount INTEGER NOT NULL, rebuildStatus TEXT NOT NULL, recordedAtEpochMillis INTEGER NOT NULL)")
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '6d801f0d762d8646786b31dedfabd138')")
            database.execSQL("INSERT INTO exercises (id, name, catalogueKey, muscleGroups, isCustom, category, loggingType, muscleWeights) VALUES (1, 'Bench Press', 'timego.seed.v1.bench-press', 'CHEST', 0, 'STRENGTH', 'WEIGHT_REPS', '')")
            database.execSQL("INSERT INTO workout_sessions (id, date, routineId, startEpochMillis, endEpochMillis) VALUES (2, 20500, NULL, 1000, 2000)")
            database.execSQL("INSERT INTO set_logs (id, sessionId, exerciseId, weightKg, reps, targetReps, loggedAtEpochMillis, durationMinutes, distanceKm, holdSeconds, targetHoldSeconds, isWarmup, addedWeightKg, rpe, targetProvenance) VALUES (3, 2, 1, 40.0, 8, 8, 1500, NULL, NULL, NULL, NULL, 0, NULL, NULL, 'UNKNOWN')")
            database.execSQL("PRAGMA user_version = 14")
        }
    }

    private companion object {
        const val TEST_DATABASE = "timego-migration-15-test"
    }
}
