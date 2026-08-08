package com.lsing.timego.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class],
    version = 1,
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
                    .build()
                    .also { instance = it }
            }
    }
}
