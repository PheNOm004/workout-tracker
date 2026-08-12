package com.lsing.timego.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SEED_EXERCISES
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.HoldPerformance
import com.lsing.timego.domain.HoldSuggestion
import com.lsing.timego.domain.OverloadSuggestion
import com.lsing.timego.domain.RuleBasedHoldSuggester
import com.lsing.timego.domain.RuleBasedOverloadSuggester
import com.lsing.timego.domain.SessionAutoCloseDecision
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.checkSessionAutoClose
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
import com.lsing.timego.domain.muscleGroupsWorkedInSession
import com.lsing.timego.domain.rankUntrainedMuscleGroups
import com.lsing.timego.domain.routinesForToday
import com.lsing.timego.ui.common.DayHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class NoActiveSession(val lastSession: LastSessionSummary?, val recommendedMuscleGroups: List<String>) : SessionUiState
    data class Active(val sessionId: Long) : SessionUiState
}

data class LastSessionSummary(
    val sets: Int,
    val muscleGroups: Set<String>,
    val durationMinutes: Long,
    val detail: List<DayHistoryEntry>,
)

/** [selectedRoutineId] null means freeform (all exercises shown, sessions logged with no routine
 *  link); non-null filters [displayedExercises] to that routine's exercises and tags logged
 *  sessions with it. On first load, if today has a scheduled routine, it's auto-selected instead
 *  of defaulting to freeform -- that's the whole point of routine scheduling (Update 1.1). */
class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private val suggester = RuleBasedOverloadSuggester()
    private val holdSuggester = RuleBasedHoldSuggester()

    private var allExercises: List<Exercise> = emptyList()
    private var hasAutoSelectedTodaysRoutine = false

    private val _displayedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val displayedExercises: StateFlow<List<Exercise>> = _displayedExercises.asStateFlow()

    private val _suggestions = MutableStateFlow<Map<Long, OverloadSuggestion>>(emptyMap())
    val suggestions: StateFlow<Map<Long, OverloadSuggestion>> = _suggestions.asStateFlow()

    private val _holdSuggestions = MutableStateFlow<Map<Long, HoldSuggestion>>(emptyMap())
    val holdSuggestions: StateFlow<Map<Long, HoldSuggestion>> = _holdSuggestions.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _selectedRoutineId = MutableStateFlow<Long?>(null)
    val selectedRoutineId: StateFlow<Long?> = _selectedRoutineId.asStateFlow()

    private val _latestBodyWeightKg = MutableStateFlow<Double?>(null)
    val latestBodyWeightKg: StateFlow<Double?> = _latestBodyWeightKg.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedMissingExercises(SEED_EXERCISES)
            repository.exercises.collect { list ->
                allExercises = list
                refreshSuggestions(list)
                refreshDisplayedExercises()
                refreshSessionState()
            }
        }
        viewModelScope.launch {
            repository.routines.collect { routineList ->
                _routines.value = routineList
                if (!hasAutoSelectedTodaysRoutine) {
                    hasAutoSelectedTodaysRoutine = true
                    routinesForToday(routineList, LocalDate.now().dayOfWeek).firstOrNull()?.let {
                        selectRoutine(it.id)
                    }
                }
            }
        }
        viewModelScope.launch {
            _latestBodyWeightKg.value = repository.latestBodyWeightKg()
        }
    }

    fun selectRoutine(routineId: Long?) {
        _selectedRoutineId.value = routineId
        viewModelScope.launch { refreshDisplayedExercises() }
    }

    private suspend fun refreshDisplayedExercises() {
        val routineId = _selectedRoutineId.value
        _displayedExercises.value = if (routineId == null) {
            allExercises
        } else {
            val exerciseIds = repository.exercisesForRoutine(routineId).map { it.exerciseId }.toSet()
            allExercises.filter { it.id in exerciseIds }
        }
    }

    /** Splits suggestion computation by loggingType: WEIGHT_REPS exercises get a weight/reps
     *  suggestion from [suggester], HOLD exercises get a duration suggestion from [holdSuggester]
     *  -- an exercise can only produce one kind, so each history is built from the fields that
     *  are real for that exercise (see SetLog's doc comment on its sentinel-field convention). */
    private suspend fun refreshSuggestions(exerciseList: List<Exercise>) {
        val historyByExercise = repository.allSetLogsOrderedByTime().groupBy { it.exerciseId }
        val map = mutableMapOf<Long, OverloadSuggestion>()
        val holdMap = mutableMapOf<Long, HoldSuggestion>()
        for (exercise in exerciseList) {
            val history = historyByExercise[exercise.id].orEmpty()
            if (exercise.loggingType == LoggingType.HOLD.name) {
                val holdHistory = history.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
                holdSuggester.suggestNext(holdHistory, exercise.name)?.let { holdMap[exercise.id] = it }
            } else {
                val performanceHistory = history.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
                suggester.suggestNext(performanceHistory)?.let { map[exercise.id] = it }
            }
        }
        _suggestions.value = map
        _holdSuggestions.value = holdMap
    }

    private suspend fun refreshSessionState() {
        val active = repository.activeSession()
        if (active != null) {
            val sets = repository.setLogsForSession(active.id)
            val lastSetTime = sets.maxOfOrNull { it.loggedAtEpochMillis }
            val decision = if (lastSetTime != null) {
                checkSessionAutoClose(lastSetTime, System.currentTimeMillis())
            } else {
                SessionAutoCloseDecision.STAY_ACTIVE // just started, nothing logged yet -- never auto-close an empty session
            }
            if (decision == SessionAutoCloseDecision.AUTO_CLOSE) {
                repository.endSession(active.id, lastSetTime!!)
                _sessionState.value = buildNoActiveSessionState()
                return
            }
            _sessionState.value = SessionUiState.Active(active.id)
        } else {
            _sessionState.value = buildNoActiveSessionState()
        }
    }

    private suspend fun buildNoActiveSessionState(): SessionUiState.NoActiveSession {
        val lastSession = repository.lastClosedSession()
        val summary = lastSession?.let { session ->
            val sets = repository.setLogsForSession(session.id)
            val exercisesById = allExercises.associateBy { it.id }
            val muscleGroups = muscleGroupsWorkedInSession(session.id, sets, allExercises)
            val detail = sets.mapNotNull { log ->
                val exercise = exercisesById[log.exerciseId] ?: return@mapNotNull null
                val description = when (exercise.loggingType) {
                    LoggingType.DURATION_DISTANCE.name -> {
                        val distance = log.distanceKm?.let { " -- ${it}km" } ?: ""
                        "${log.durationMinutes ?: 0.0} min$distance"
                    }
                    LoggingType.HOLD.name -> "${log.holdSeconds ?: 0}s hold"
                    else -> "${log.weightKg}kg x ${log.reps}"
                }
                DayHistoryEntry(exercise.name, description)
            }
            LastSessionSummary(
                sets = sets.size,
                muscleGroups = muscleGroups,
                durationMinutes = (session.endEpochMillis ?: session.startEpochMillis).minus(session.startEpochMillis) / 60_000,
                detail = detail,
            )
        }

        val allSets = repository.allSetLogs()
        val sessionDateById = repository.allSessions().associate { it.id to it.date }
        val exercisesById = allExercises.associateBy { it.id }
        val lastTrained = lastTrainedDatesByMuscleGroup(allSets, exercisesById, sessionDateById)
        val allGroups = MuscleGroup.entries.map { it.name }
        val recommended = rankUntrainedMuscleGroups(allGroups, lastTrained, LocalDate.now()).take(2)

        return SessionUiState.NoActiveSession(lastSession = summary, recommendedMuscleGroups = recommended)
    }

    fun startSession(routineId: Long?) {
        viewModelScope.launch {
            val session = repository.startSession(routineId)
            selectRoutine(routineId)
            _sessionState.value = SessionUiState.Active(session.id)
        }
    }

    fun endActiveSession() {
        val current = _sessionState.value
        if (current !is SessionUiState.Active) return
        viewModelScope.launch {
            repository.endSession(current.sessionId, System.currentTimeMillis())
            _sessionState.value = buildNoActiveSessionState()
        }
    }

    fun logSet(exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logSet(sessionId, exerciseId, weightKg, reps, targetReps)
            refreshSuggestionForExercise(exerciseId)
        }
    }

    fun logCardioSet(exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logCardioSet(sessionId, exerciseId, durationMinutes, distanceKm)
        }
    }

    fun logHoldSet(exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logHoldSet(sessionId, exerciseId, durationSeconds, targetDurationSeconds)
            refreshSuggestionForExercise(exerciseId)
        }
    }

    /** Recomputes the suggestion for just the exercise that was logged, instead of every exercise
     *  in the library ([refreshSuggestions] used to run after every single logged set -- a full
     *  history rescan grouped across all 585 seeded exercises just to update one row, which is why
     *  logging felt slow). Only that exercise's own history can have changed, so only its map entry
     *  needs to move; every other exercise's suggestion (and the suggestions for exercises that
     *  aren't even on screen right now) is untouched. */
    private suspend fun refreshSuggestionForExercise(exerciseId: Long) {
        val exercise = allExercises.firstOrNull { it.id == exerciseId } ?: return
        val history = repository.historyForExercise(exerciseId)
        if (exercise.loggingType == LoggingType.HOLD.name) {
            val holdHistory = history.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
            val suggestion = holdSuggester.suggestNext(holdHistory, exercise.name)
            _holdSuggestions.value = if (suggestion != null) {
                _holdSuggestions.value + (exerciseId to suggestion)
            } else {
                _holdSuggestions.value - exerciseId
            }
        } else {
            val performanceHistory = history.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            val suggestion = suggester.suggestNext(performanceHistory)
            _suggestions.value = if (suggestion != null) {
                _suggestions.value + (exerciseId to suggestion)
            } else {
                _suggestions.value - exerciseId
            }
        }
    }

    fun addCustomExercise(name: String, muscleGroups: List<String>, category: String) {
        viewModelScope.launch {
            repository.addCustomExercise(name, muscleGroups, category)
        }
    }
}
