package com.lsing.timego.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import com.lsing.timego.domain.planBackupMerge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

const val TIMEGO_BACKUP_MIME_TYPE = "application/vnd.sqlite3"
internal const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L
private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

internal fun InputStream.copyBackupTo(output: OutputStream, maxBytes: Long = MAX_BACKUP_BYTES): Long {
    require(maxBytes >= 0L) { "Backup size limit cannot be negative" }
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L
    while (true) {
        val bytesRead = read(buffer)
        if (bytesRead < 0) break
        if (bytesRead == 0) continue
        if (totalBytes > maxBytes - bytesRead) {
            error("Backup exceeds the allowed restore size")
        }
        output.write(buffer, 0, bytesRead)
        totalBytes += bytesRead
    }
    return totalBytes
}

internal fun File.hasSqliteHeader(): Boolean = inputStream().use { input ->
    val actual = ByteArray(SQLITE_HEADER.size)
    var offset = 0
    while (offset < actual.size) {
        val bytesRead = input.read(actual, offset, actual.size - offset)
        if (bytesRead < 0) return@use false
        if (bytesRead > 0) offset += bytesRead
    }
    actual.contentEquals(SQLITE_HEADER)
}

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

/** App-local backup and restore with no TimeGo network access. Export writes a checkpointed copy
 *  of the live database to a user-chosen Storage Access Framework location outside app storage;
 *  the selected document provider controls whether that location is local or cloud-backed.
 *  Restore never overwrites: it opens the chosen backup as a temporary Room database, diffs it
 *  against live data with [planBackupMerge], and only inserts what live data doesn't already have. */
class BackupManager(private val context: Context, private val db: TimeGoDatabase) {

    suspend fun export(destination: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Checkpoints WAL into the main file so a plain copy of timego.db alone is a complete,
            // consistent snapshot -- same technique already used for manual device-data edits.
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val liveDbFile = context.getDatabasePath(TIMEGO_DATABASE_FILE_NAME)
            val out = context.contentResolver.openOutputStream(destination)
                ?: error("Could not open the chosen location for writing")
            out.use { stream -> liveDbFile.inputStream().use { it.copyTo(stream) } }
            Unit
        }
    }

    suspend fun restore(source: Uri): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File.createTempFile("timego-restore-", ".db", context.cacheDir)
            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: error("Could not open the chosen file for reading")
                input.use { stream -> tempFile.outputStream().use { stream.copyBackupTo(it) } }
                check(tempFile.hasSqliteHeader()) { "The selected file is not a TimeGo SQLite backup" }

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
                        backupRoutineExercises = backupRoutines.flatMap {
                            backupDb.routineDao().exercisesForRoutine(it.id)
                        },
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
                                    RoutineExercise(
                                        routineId = newRoutineId,
                                        exerciseId = exerciseId,
                                        orderIndex = index,
                                    ),
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
}
