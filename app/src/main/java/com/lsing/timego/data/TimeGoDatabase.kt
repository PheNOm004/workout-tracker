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

@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class],
    version = 3,
    exportSchema = false,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
