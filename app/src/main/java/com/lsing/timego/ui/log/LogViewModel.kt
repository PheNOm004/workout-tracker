package com.lsing.timego.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SEED_EXERCISES
import com.lsing.timego.data.SettingsRepository
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
import com.lsing.timego.domain.isCardioOnlySession
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
import com.lsing.timego.domain.muscleGroupsWorkedInSession
import com.lsing.timego.domain.rankUntrainedMuscleGroups
import com.lsing.timego.domain.routinesForToday
import com.lsing.timego.domain.sessionWorkingSetHistory
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.buildDayHistoryEntries
import com.lsing.timego.ui.common.sessionDayLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object NoActiveSession : SessionUiState
    data class Active(val sessionId: Long) : SessionUiState
}

data class LastSessionSummary(
    val sets: Int,
    val muscleGroups: Set<String>,
    val label: String,
    val durationMinutes: Long,
    val detail: List<DayHistoryEntry>,
)

/** Last-session summary + recommended muscle groups -- kept fresh independently of
 *  [SessionUiState] so the landing page's content (last session, recommendation) can still be
 *  shown when the user backs out of an in-progress session to peek at it, not just when there's
 *  genuinely no active session. */
data class LandingSummary(
    val lastSession: LastSessionSummary?,
    val recommendedMuscleGroups: List<String>,
)

/** [selectedRoutineId] null means freeform (all exercises shown, sessions logged with no routine
 *  link); non-null filters [displayedExercises] to that routine's exercises and tags logged
 *  sessions with it. On first load, if today has a scheduled routine, it's auto-selected instead
 *  of defaulting to freeform -- that's the whole point of routine scheduling (Update 1.1). */
class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private val settingsRepository = SettingsRepository(application)
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

    private val _landingSummary = MutableStateFlow(LandingSummary(lastSession = null, recommendedMuscleGroups = emptyList()))
    val landingSummary: StateFlow<LandingSummary> = _landingSummary.asStateFlow()

    private val _holdDelaySeconds = MutableStateFlow(SettingsRepository.DEFAULT_HOLD_DELAY_SECONDS)
    val holdDelaySeconds: StateFlow<Int> = _holdDelaySeconds.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.holdDelaySeconds.collect { _holdDelaySeconds.value = it }
        }
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
     *  suggestion from [suggester], HOLD exercises get a duration suggestion from [holdSuggester] --
     *  an exercise can only produce one kind, so each history is built from the fields that are real
     *  for that exercise (see SetLog's doc comment on its sentinel-field convention). Each exercise's
     *  raw sets are reduced to [sessionWorkingSetHistory] (one representative set per past session)
     *  plus, separately, the active session's own working sets for that exercise so far -- see the
     *  2026-08-12 warmup-session-aware-suggester design for why suggestions no longer look at a flat
     *  raw-set history. */
    private suspend fun refreshSuggestions(exerciseList: List<Exercise>) {
        val allSets = repository.allSetLogsOrderedByTime()
        val sessionStartById = repository.allSessions().associate { it.id to it.startEpochMillis }
        val activeSessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId
        val setsByExercise = allSets.groupBy { it.exerciseId }
        val map = mutableMapOf<Long, OverloadSuggestion>()
        val holdMap = mutableMapOf<Long, HoldSuggestion>()
        for (exercise in exerciseList) {
            val exerciseSets = setsByExercise[exercise.id].orEmpty()
            val sessionHistory = sessionWorkingSetHistory(exerciseSets, sessionStartById)
            val currentSessionWorkingSets = if (activeSessionId != null) {
                exerciseSets.filter { it.sessionId == activeSessionId && !it.isWarmup }.sortedBy { it.loggedAtEpochMillis }
            } else {
                emptyList()
            }
            if (exercise.loggingType == LoggingType.HOLD.name) {
                val historyPerf = sessionHistory.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
                val currentPerf = currentSessionWorkingSets.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
                holdSuggester.suggestNext(historyPerf, currentPerf, exercise.name)?.let { holdMap[exercise.id] = it }
            } else {
                val historyPerf = sessionHistory.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
                val currentPerf = currentSessionWorkingSets.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
                suggester.suggestNext(historyPerf, currentPerf)?.let { map[exercise.id] = it }
            }
        }
        _suggestions.value = map
        _holdSuggestions.value = holdMap
    }

    private suspend fun refreshSessionState() {
        refreshLandingSummary()
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
                refreshLandingSummary() // re-fetch so the just-closed session shows as "last session"
                _sessionState.value = SessionUiState.NoActiveSession
                return
            }
            _sessionState.value = SessionUiState.Active(active.id)
        } else {
            _sessionState.value = SessionUiState.NoActiveSession
        }
    }

    /** Kept independent of [refreshSessionState] so it can also be called right after
     *  [endActiveSession] closes a session, before [SessionUiState] itself changes -- the landing
     *  page's last-session card should reflect the session that was just ended, and this is the
     *  only place that recomputes it. */
    private suspend fun refreshLandingSummary() {
        val lastSession = repository.lastClosedSession()
        val summary = lastSession?.let { session ->
            val sets = repository.setLogsForSession(session.id)
            val exercisesById = allExercises.associateBy { it.id }
            val muscleGroups = muscleGroupsWorkedInSession(session.id, sets, allExercises)
            val detail = buildDayHistoryEntries(sets, exercisesById)
            LastSessionSummary(
                sets = sets.size,
                muscleGroups = muscleGroups,
                label = sessionDayLabel(muscleGroups, isCardioOnlySession(sets, exercisesById)),
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

        _landingSummary.value = LandingSummary(lastSession = summary, recommendedMuscleGroups = recommended)
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
            refreshLandingSummary()
            _sessionState.value = SessionUiState.NoActiveSession
        }
    }

    fun logSet(exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int, isWarmup: Boolean = false, addedWeightKg: Double? = null) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logSet(sessionId, exerciseId, weightKg, reps, targetReps, isWarmup, addedWeightKg)
            refreshSuggestionForExercise(exerciseId)
        }
    }

    fun logCardioSet(exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logCardioSet(sessionId, exerciseId, durationMinutes, distanceKm)
        }
    }

    fun logHoldSet(exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean = false) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logHoldSet(sessionId, exerciseId, durationSeconds, targetDurationSeconds, isWarmup)
            refreshSuggestionForExercise(exerciseId)
        }
    }

    /** Recomputes the suggestion for just the exercise that was logged, instead of every exercise
     *  in the library (see [refreshSuggestions]'s doc comment for why -- unchanged perf rationale
     *  from the 2026-08-10 logging-field-accuracy session). */
    private suspend fun refreshSuggestionForExercise(exerciseId: Long) {
        val exercise = allExercises.firstOrNull { it.id == exerciseId } ?: return
        val (sessionHistory, currentSessionWorkingSets) = buildSuggestionInputs(exerciseId)
        if (exercise.loggingType == LoggingType.HOLD.name) {
            val historyPerf = sessionHistory.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
            val currentPerf = currentSessionWorkingSets.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
            val suggestion = holdSuggester.suggestNext(historyPerf, currentPerf, exercise.name)
            _holdSuggestions.value = if (suggestion != null) {
                _holdSuggestions.value + (exerciseId to suggestion)
            } else {
                _holdSuggestions.value - exerciseId
            }
        } else {
            val historyPerf = sessionHistory.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            val currentPerf = currentSessionWorkingSets.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            val suggestion = suggester.suggestNext(historyPerf, currentPerf)
            _suggestions.value = if (suggestion != null) {
                _suggestions.value + (exerciseId to suggestion)
            } else {
                _suggestions.value - exerciseId
            }
        }
    }

    /** Shared by [refreshSuggestionForExercise] -- fetches this exercise's full history once,
     *  splits it into past-session representative performances and the active session's own
     *  working sets so far (empty if no session is active, per [SessionUiState]). */
    private suspend fun buildSuggestionInputs(exerciseId: Long): Pair<List<com.lsing.timego.data.SetLog>, List<com.lsing.timego.data.SetLog>> {
        val allSets = repository.historyForExercise(exerciseId)
        val sessionStartById = repository.allSessions().associate { it.id to it.startEpochMillis }
        val sessionHistory = sessionWorkingSetHistory(allSets, sessionStartById)
        val activeSessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId
        val currentSessionWorkingSets = if (activeSessionId != null) {
            allSets.filter { it.sessionId == activeSessionId && !it.isWarmup }.sortedBy { it.loggedAtEpochMillis }
        } else {
            emptyList()
        }
        return sessionHistory to currentSessionWorkingSets
    }

    fun addCustomExercise(name: String, muscleGroups: List<String>, category: String) {
        viewModelScope.launch {
            repository.addCustomExercise(name, muscleGroups, category)
        }
    }
}
