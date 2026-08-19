package com.lsing.timego.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SettingsRepository
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.exerciseUsageFrequency
import com.lsing.timego.domain.exercisesRankedByFrequency
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
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

class RoutinesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private val settingsRepository = SettingsRepository(application)

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
        val allGroups = MuscleGroup.entries.map { it.name }
        _untrainedGroups.value = untrainedMuscleGroups(allGroups, lastTrained, LocalDate.now())
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
}
