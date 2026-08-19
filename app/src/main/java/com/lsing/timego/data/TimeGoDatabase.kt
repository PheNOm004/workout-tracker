package com.lsing.timego.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lsing.timego.data.adaptive.ShadowAuditEntity
import com.lsing.timego.data.adaptive.ShadowDao
import com.lsing.timego.data.adaptive.ShadowSnapshotEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN category TEXT NOT NULL DEFAULT 'STRENGTH'")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN durationMinutes REAL")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN distanceKm REAL")
        db.execSQL("ALTER TABLE routines ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE body_metrics ADD COLUMN heightCm REAL")
    }
}

/** No-op at the SQL level -- the underlying columns and their defaults already exist from
 *  MIGRATION_1_2. This migration exists purely so Room re-stamps the schema identity hash after
 *  Exercise.category/Routine.daysOfWeek gained @ColumnInfo(defaultValue=...) annotations: adding
 *  that annotation changes the *computed* schema even though no DDL changed, and Room refuses to
 *  open a database whose stored hash doesn't match unless the version number also moved. Skipping
 *  this bump is exactly what broke every existing install (including this device) after that
 *  annotation was added without a matching version bump. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN loggingType TEXT NOT NULL DEFAULT 'WEIGHT_REPS'")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN holdSeconds INTEGER")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN targetHoldSeconds INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN muscleWeights TEXT NOT NULL DEFAULT ''")
    }
}

/** Backfills startEpochMillis/endEpochMillis from each session's own set_logs (min/max
 *  loggedAtEpochMillis) rather than leaving them at a fixed default -- pre-migration sessions have
 *  no explicit boundaries, so their nearest real signal is the timestamps of the sets actually
 *  logged in them. A session with zero sets (shouldn't normally happen, but not impossible if a
 *  session was created and nothing was ever logged) falls back to midnight of its date column
 *  (`date` is stored as an epoch-day Long via Converters.toEpochDay, so `date * 86400000` is that
 *  day's midnight in epoch millis). Every backfilled row gets a non-null endEpochMillis --
 *  pre-migration data has no concept of "still active," so all of it is treated as closed. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN startEpochMillis INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN endEpochMillis INTEGER")
        db.execSQL(
            """
            UPDATE workout_sessions SET
                startEpochMillis = COALESCE(
                    (SELECT MIN(loggedAtEpochMillis) FROM set_logs WHERE set_logs.sessionId = workout_sessions.id),
                    date * 86400000
                ),
                endEpochMillis = COALESCE(
                    (SELECT MAX(loggedAtEpochMillis) FROM set_logs WHERE set_logs.sessionId = workout_sessions.id),
                    date * 86400000
                )
            """,
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_logs ADD COLUMN isWarmup INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_logs ADD COLUMN addedWeightKg REAL")
    }
}

/** Pure index addition -- no column or row changes. Index names must match exactly what Room
 *  generates from the @Index annotations (`index_<table>_<column>`), or the schema validator
 *  rejects the migrated database on open. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_set_logs_sessionId` ON `set_logs` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_set_logs_exerciseId` ON `set_logs` (`exerciseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercises_routineId` ON `routine_exercises` (`routineId`)")
    }
}

/** Pure additive column, default NULL -- existing rows read back with rpe == null, no backfill. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_logs ADD COLUMN rpe INTEGER")
    }
}

/** Pure additive evidence field. Existing target values had no pre-set provenance, so migration
 * deliberately marks every historic row UNKNOWN rather than inventing achieved/missed labels. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_logs ADD COLUMN targetProvenance TEXT NOT NULL DEFAULT 'UNKNOWN'")
    }
}

/** Curated seed keys are populated by the existing startup seed synchronisation. Custom rows stay
 * null and are excluded from coach analysis until a declarative mapping is explicitly reviewed. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN catalogueKey TEXT")
    }
}

/** Strictly additive derived-cache storage. Canonical exercise, session, and set rows are never
 * altered or backfilled: a missing cache is rebuilt from those existing rows on demand. */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shadow_snapshots` (" +
                "`cacheKey` TEXT NOT NULL, `sourceFingerprint` TEXT NOT NULL, " +
                "`modelContractHash` TEXT NOT NULL, `metadataHash` TEXT NOT NULL, " +
                "`orderingPolicy` TEXT NOT NULL, `statePayload` TEXT NOT NULL, " +
                "`sourceRowCount` INTEGER NOT NULL, `observationCount` INTEGER NOT NULL, " +
                "`exclusionCount` INTEGER NOT NULL, `completionStatus` TEXT NOT NULL, " +
                "`completedAtEpochMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`cacheKey`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shadow_audit` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceFingerprint` TEXT NOT NULL, " +
                "`modelContractHash` TEXT NOT NULL, `metadataHash` TEXT NOT NULL, " +
                "`orderingPolicy` TEXT NOT NULL, `sourceRowCount` INTEGER NOT NULL, " +
                "`observationCount` INTEGER NOT NULL, `exclusionCount` INTEGER NOT NULL, " +
                "`rebuildStatus` TEXT NOT NULL, `recordedAtEpochMillis` INTEGER NOT NULL)",
        )
    }
}

@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class, ShadowSnapshotEntity::class, ShadowAuditEntity::class],
    version = 14,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TimeGoDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun routineDao(): RoutineDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun shadowDao(): ShadowDao

    companion object {
        @Volatile private var instance: TimeGoDatabase? = null

        fun getInstance(context: Context): TimeGoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, TimeGoDatabase::class.java, "timego.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .build()
                    .also { instance = it }
            }
    }
}
