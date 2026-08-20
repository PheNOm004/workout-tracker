package com.lsing.timego.domain

import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.RoutineExercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession

/** One closed session, ready to insert (`id = 0`), with its sets already resolved to live
 *  exercise ids (also `id = 0`, `sessionId` left as the *backup* session id -- the caller inserts
 *  [session] first, then re-stamps each set's `sessionId` to the freshly assigned live id before
 *  inserting the sets). */
data class BackupSessionImport(val session: WorkoutSession, val sets: List<SetLog>)

/** A routine ready to insert (`id = 0`), with its exercise ids already resolved to the live
 *  database and kept in original order. */
data class BackupRoutineImport(val routine: Routine, val exerciseIds: List<Long>)

/** Everything a backup restore can safely add on top of live data, plus counts of what it
 *  deliberately left alone. Never contains an instruction to delete or overwrite a live row --
 *  restore is additive-only by construction. */
data class BackupMergePlan(
    val sessionsToImport: List<BackupSessionImport>,
    val skippedSessionCount: Int,
    val routinesToImport: List<BackupRoutineImport>,
    val skippedRoutineCount: Int,
    val bodyMetricsToImport: List<BodyMetric>,
    val skippedBodyMetricCount: Int,
    val skippedSetCountUnknownExercise: Int,
)

/** Resolves a backup exercise to its live-database equivalent by stable identity: catalogue key
 *  for built-ins, exact name for custom exercises (the same convention
 *  [com.lsing.timego.data.WorkoutRepository.seedMissingExercises] already uses for matching seed
 *  rows) -- never by raw Room id, which is only guaranteed stable within one export, not across
 *  two separate ones. */
private fun resolveLiveExerciseId(
    backupExercise: Exercise,
    liveByCatalogueKey: Map<String, Exercise>,
    liveByName: Map<String, Exercise>,
): Long? {
    backupExercise.catalogueKey?.let { key -> liveByCatalogueKey[key]?.let { return it.id } }
    return liveByName[backupExercise.name]?.id
}

/** Plans a restore as a pure diff against live data: a backup session is imported only when no
 *  live session shares its [WorkoutSession.startEpochMillis] (a real historical timestamp, so a
 *  collision between two genuinely different sessions is not a realistic concern); an already-
 *  present session is skipped whole rather than merged set-by-set, so a restore can never mutate
 *  or duplicate rows that already exist live. Routines dedupe by name, body metrics by date. */
fun planBackupMerge(
    backupSessions: List<WorkoutSession>,
    backupSetLogs: List<SetLog>,
    backupExercises: List<Exercise>,
    backupRoutines: List<Routine>,
    backupRoutineExercises: List<RoutineExercise>,
    backupBodyMetrics: List<BodyMetric>,
    liveSessions: List<WorkoutSession>,
    liveExercises: List<Exercise>,
    liveRoutines: List<Routine>,
    liveBodyMetrics: List<BodyMetric>,
): BackupMergePlan {
    val liveStartTimes = liveSessions.map { it.startEpochMillis }.toSet()
    val liveRoutineNames = liveRoutines.map { it.name }.toSet()
    val liveDates = liveBodyMetrics.map { it.date }.toSet()
    val liveByCatalogueKey = liveExercises.filter { it.catalogueKey != null }.associateBy { it.catalogueKey!! }
    val liveByName = liveExercises.associateBy { it.name }
    val backupExercisesById = backupExercises.associateBy { it.id }
    val setsBySession = backupSetLogs.groupBy { it.sessionId }

    var skippedSets = 0
    val sessionImports = backupSessions
        .filter { it.startEpochMillis !in liveStartTimes }
        .map { backupSession ->
            val sets = setsBySession[backupSession.id].orEmpty().mapNotNull { set ->
                val liveExerciseId = backupExercisesById[set.exerciseId]
                    ?.let { resolveLiveExerciseId(it, liveByCatalogueKey, liveByName) }
                if (liveExerciseId == null) {
                    skippedSets++
                    null
                } else {
                    set.copy(id = 0, exerciseId = liveExerciseId)
                }
            }
            BackupSessionImport(session = backupSession.copy(id = 0), sets = sets)
        }
    val skippedSessionCount = backupSessions.size - sessionImports.size

    val backupRoutineExercisesByRoutine = backupRoutineExercises.groupBy { it.routineId }
    val routineImports = backupRoutines
        .filter { it.name !in liveRoutineNames }
        .map { routine ->
            val exerciseIds = backupRoutineExercisesByRoutine[routine.id].orEmpty()
                .sortedBy { it.orderIndex }
                .mapNotNull { entry ->
                    backupExercisesById[entry.exerciseId]?.let { resolveLiveExerciseId(it, liveByCatalogueKey, liveByName) }
                }
            BackupRoutineImport(routine = routine.copy(id = 0), exerciseIds = exerciseIds)
        }
    val skippedRoutineCount = backupRoutines.size - routineImports.size

    val bodyMetricImports = backupBodyMetrics.filter { it.date !in liveDates }.map { it.copy(id = 0) }
    val skippedBodyMetricCount = backupBodyMetrics.size - bodyMetricImports.size

    return BackupMergePlan(
        sessionsToImport = sessionImports,
        skippedSessionCount = skippedSessionCount,
        routinesToImport = routineImports,
        skippedRoutineCount = skippedRoutineCount,
        bodyMetricsToImport = bodyMetricImports,
        skippedBodyMetricCount = skippedBodyMetricCount,
        skippedSetCountUnknownExercise = skippedSets,
    )
}
