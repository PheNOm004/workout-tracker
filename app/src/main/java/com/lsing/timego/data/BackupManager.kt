package com.lsing.timego.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import com.lsing.timego.domain.planBackupMerge
import kotlinx.coroutines.flow.first
import java.io.File

/** Outcome of a [BackupManager.restore] call -- always additive, so every count here describes
 *  either something newly added or something deliberately left untouched, never anything removed
 *  or overwritten. */
data class RestoreSummary(
    val importedSessions: Int,
    val importedSets: Int,
    val skippedSessions: Int,
    val importedRoutines: Int,
    val skippedRoutines: Int,
    val importedBodyMetrics: Int,
    val skippedBodyMetrics: Int,
    val skippedSetsUnknownExercise: Int,
)

/** Local-only backup and restore. Export writes a checkpointed copy of the live database to a
 *  user-chosen Storage Access Framework location -- outside the app's private storage, so an
 *  uninstall (which wiped live data once already; see the 2026-08-20 incident) can't touch it, and
 *  with no cloud/network involvement. Restore never overwrites: it opens the chosen backup file as
 *  its own temporary Room database, diffs it against live data with [planBackupMerge], and only
 *  inserts what live data doesn't already have. */
class BackupManager(private val context: Context, private val db: TimeGoDatabase) {

    suspend fun export(destination: Uri): Result<Unit> = runCatching {
        // Checkpoints WAL into the main file so a plain copy of timego.db alone is a complete,
        // consistent snapshot -- same technique already used for manual device-data edits.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val liveDbFile = context.getDatabasePath(TIMEGO_DATABASE_FILE_NAME)
        val out = context.contentResolver.openOutputStream(destination)
            ?: error("Could not open the chosen location for writing")
        out.use { stream -> liveDbFile.inputStream().use { it.copyTo(stream) } }
    }

    suspend fun restore(source: Uri): Result<RestoreSummary> = runCatching {
        val tempFile = File.createTempFile("timego-restore-", ".db", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: error("Could not open the chosen file for reading")
            input.use { stream -> tempFile.outputStream().use { stream.copyTo(it) } }

            val backupDb = Room.databaseBuilder(context, TimeGoDatabase::class.java, tempFile.absolutePath)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
            try {
                val backupRoutines = backupDb.routineDao().observeRoutines().first()
                val plan = planBackupMerge(
                    backupSessions = backupDb.sessionDao().observeAll().first(),
                    backupSetLogs = backupDb.setLogDao().getAll(),
                    backupExercises = backupDb.exerciseDao().observeAll().first(),
                    backupRoutines = backupRoutines,
                    backupRoutineExercises = backupRoutines.flatMap { backupDb.routineDao().exercisesForRoutine(it.id) },
                    backupBodyMetrics = backupDb.bodyMetricDao().observeAll().first(),
                    liveSessions = db.sessionDao().observeAll().first(),
                    liveExercises = db.exerciseDao().observeAll().first(),
                    liveRoutines = db.routineDao().observeRoutines().first(),
                    liveBodyMetrics = db.bodyMetricDao().observeAll().first(),
                )

                var importedSets = 0
                db.withTransaction {
                    plan.sessionsToImport.forEach { sessionImport ->
                        val newSessionId = db.sessionDao().insert(sessionImport.session)
                        sessionImport.sets.forEach { set ->
                            db.setLogDao().insert(set.copy(sessionId = newSessionId))
                            importedSets++
                        }
                    }
                    plan.routinesToImport.forEach { routineImport ->
                        val newRoutineId = db.routineDao().insertRoutine(routineImport.routine)
                        routineImport.exerciseIds.forEachIndexed { index, exerciseId ->
                            db.routineDao().insertRoutineExercise(
                                RoutineExercise(routineId = newRoutineId, exerciseId = exerciseId, orderIndex = index),
                            )
                        }
                    }
                    plan.bodyMetricsToImport.forEach { db.bodyMetricDao().insert(it) }
                }

                RestoreSummary(
                    importedSessions = plan.sessionsToImport.size,
                    importedSets = importedSets,
                    skippedSessions = plan.skippedSessionCount,
                    importedRoutines = plan.routinesToImport.size,
                    skippedRoutines = plan.skippedRoutineCount,
                    importedBodyMetrics = plan.bodyMetricsToImport.size,
                    skippedBodyMetrics = plan.skippedBodyMetricCount,
                    skippedSetsUnknownExercise = plan.skippedSetCountUnknownExercise,
                )
            } finally {
                backupDb.close()
            }
        } finally {
            tempFile.delete()
            File(tempFile.parentFile, "${tempFile.name}-wal").delete()
            File(tempFile.parentFile, "${tempFile.name}-shm").delete()
        }
    }
}
