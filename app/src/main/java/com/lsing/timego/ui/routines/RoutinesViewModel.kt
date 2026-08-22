package com.lsing.timego.ui.routines

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.BackupManager
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.RestoreSummary
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SettingsRepository
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.exerciseUsageFrequency
import com.lsing.timego.domain.exercisesRankedByFrequency
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
import com.lsing.timego.domain.rankUntrainedMuscleGroups
import com.lsing.timego.domain.untrainedMuscleGroups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One closed past session, for the Routines page's deletable session-history list. Deliberately
 *  excludes the active session (endEpochMillis == null) -- see [WorkoutRepository.deleteSession]'s
 *  doc comment for why deleting an in-progress session isn't a supported case. */
data class SessionHistoryEntry(val id: Long, val date: LocalDate, val setCount: Int)

/** Outcome of the last export/restore action, shown once as a dialog then cleared. [message] is
 *  a plain, user-readable summary; [isError] only styles the dialog, it doesn't change what's
 *  already been safely written -- export/restore either fully succeed or make no live change. */
data class BackupResult(val message: String, val isError: Boolean)

class RoutinesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TimeGoDatabase.getInstance(application)
    private val repository = WorkoutRepository(database)
    private val settingsRepository = SettingsRepository(application)
    private val backupManager = BackupManager(application, database)

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _untrainedGroups = MutableStateFlow<List<String>>(emptyList())
    val untrainedGroups: StateFlow<List<String>> = _untrainedGroups.asStateFlow()

    private val _holdDelaySeconds = MutableStateFlow(SettingsRepository.DEFAULT_HOLD_DELAY_SECONDS)
    val holdDelaySeconds: StateFlow<Int> = _holdDelaySeconds.asStateFlow()

    private val _trainingLean = MutableStateFlow(TrainingLean.BALANCED)
    val trainingLean: StateFlow<TrainingLean> = _trainingLean.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<SessionHistoryEntry>>(emptyList())
    val sessionHistory: StateFlow<List<SessionHistoryEntry>> = _sessionHistory.asStateFlow()

    private val _backupResult = MutableStateFlow<BackupResult?>(null)
    val backupResult: StateFlow<BackupResult?> = _backupResult.asStateFlow()

    init {
        viewModelScope.launch { repository.routines.collect { _routines.value = it } }
        viewModelScope.launch {
            combine(repository.exercises, repository.setLogs) { exerciseList, setLogs ->
                exerciseList to setLogs
            }.collect { (exerciseList, setLogs) ->
                _exercises.value = exercisesRankedByFrequency(
                    exerciseList,
                    exerciseUsageFrequency(setLogs, exerciseList.associateBy { it.id }),
                )
                refreshUntrainedGroups(exerciseList)
            }
        }
        viewModelScope.launch {
            settingsRepository.holdDelaySeconds.collect { _holdDelaySeconds.value = it }
        }
        viewModelScope.launch {
            settingsRepository.trainingLean.collect { _trainingLean.value = it }
        }
        viewModelScope.launch {
            combine(repository.sessions, repository.setLogs) { sessions, setLogs ->
                val countsBySession = setLogs.groupingBy { it.sessionId }.eachCount()
                sessions
                    .filter { it.endEpochMillis != null }
                    .sortedByDescending { it.endEpochMillis }
                    .map { SessionHistoryEntry(it.id, it.date, countsBySession[it.id] ?: 0) }
            }.collect { _sessionHistory.value = it }
        }
    }

    fun setHoldDelaySeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setHoldDelaySeconds(seconds.coerceIn(0, 30)) }
    }

    fun setTrainingLean(lean: TrainingLean) {
        viewModelScope.launch { settingsRepository.setTrainingLean(lean) }
    }

    private suspend fun refreshUntrainedGroups(exerciseList: List<Exercise>) {
        val allSets = repository.allSetLogs()
        val sessionDateById = repository.allSessions().associate { it.id to it.date }
        val exercisesById = exerciseList.associateBy { it.id }
        val lastTrained = lastTrainedDatesByMuscleGroup(allSets, exercisesById, sessionDateById)
        val allGroups = MuscleGroup.entries.filterNot { it == MuscleGroup.FULL_BODY }.map { it.name }
        val staleGroups = untrainedMuscleGroups(allGroups, lastTrained, LocalDate.now())
        _untrainedGroups.value = rankUntrainedMuscleGroups(staleGroups, lastTrained, LocalDate.now())
    }

    fun createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>) {
        viewModelScope.launch { repository.createRoutine(name, exerciseIds, daysOfWeek) }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch { repository.deleteRoutine(routineId) }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteSession(sessionId) }
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            backupManager.export(destination).fold(
                onSuccess = { _backupResult.value = BackupResult("Backup saved.", isError = false) },
                onFailure = { e -> _backupResult.value = BackupResult("Export failed: ${e.message}", isError = true) },
            )
        }
    }

    fun restoreBackup(source: Uri) {
        viewModelScope.launch {
            backupManager.restore(source).fold(
                onSuccess = { summary -> _backupResult.value = BackupResult(summary.toMessage(), isError = false) },
                onFailure = { e -> _backupResult.value = BackupResult("Restore failed: ${e.message}", isError = true) },
            )
        }
    }

    fun clearBackupResult() {
        _backupResult.value = null
    }

    private fun RestoreSummary.toMessage(): String {
        val parts = mutableListOf(
            "Imported $importedSessions session${if (importedSessions == 1) "" else "s"} ($importedSets sets)",
        )
        if (skippedSessions > 0) parts += "$skippedSessions session${if (skippedSessions == 1) "" else "s"} already present, skipped"
        if (importedRoutines > 0) parts += "$importedRoutines routine${if (importedRoutines == 1) "" else "s"} imported"
        if (skippedRoutines > 0) parts += "$skippedRoutines routine${if (skippedRoutines == 1) "" else "s"} already present, skipped"
        if (importedBodyMetrics > 0) parts += "$importedBodyMetrics body metric${if (importedBodyMetrics == 1) "" else "s"} imported"
        if (skippedBodyMetrics > 0) parts += "$skippedBodyMetrics body metric${if (skippedBodyMetrics == 1) "" else "s"} already present, skipped"
        if (skippedSetsUnknownExercise > 0) parts += "$skippedSetsUnknownExercise set${if (skippedSetsUnknownExercise == 1) "" else "s"} skipped (exercise not found)"
        return parts.joinToString(". ") + "."
    }
}
