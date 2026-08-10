package com.lsing.timego.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TimeGoDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun routineDao(): RoutineDao
    abstract fun bodyMetricDao(): BodyMetricDao

    companion object {
        @Volatile private var instance: TimeGoDatabase? = null

        fun getInstance(context: Context): TimeGoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, TimeGoDatabase::class.java, "timego.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
