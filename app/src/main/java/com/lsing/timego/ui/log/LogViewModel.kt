package com.lsing.timego.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TargetProvenance
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SEED_EXERCISES
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.SettingsRepository
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.DEFAULT_WEIGHT_INCREMENT_KG
import com.lsing.timego.domain.HoldPerformance
import com.lsing.timego.domain.HoldSuggestion
import com.lsing.timego.domain.OverloadSuggestion
import com.lsing.timego.domain.RepRange
import com.lsing.timego.domain.RuleBasedHoldSuggester
import com.lsing.timego.domain.RuleBasedOverloadSuggester
import com.lsing.timego.domain.SessionAutoCloseDecision
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.checkSessionAutoClose
import com.lsing.timego.domain.expandMuscleGroupRegions
import com.lsing.timego.domain.exerciseUsageFrequency
import com.lsing.timego.domain.exercisesRankedByFrequency
import com.lsing.timego.domain.isCardioOnlySession
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
import com.lsing.timego.domain.lastWorkingSetByExercise
import com.lsing.timego.domain.latestWeightKg
import com.lsing.timego.domain.muscleBalanceForTimeframe
import com.lsing.timego.domain.muscleGroupsAffectedInSession
import com.lsing.timego.domain.muscleGroupsWorkedInSession
import com.lsing.timego.domain.muscleGroupIntensityForSession
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.domain.rankUntrainedMuscleGroups
import com.lsing.timego.domain.repRangeAtWeight
import com.lsing.timego.domain.routineLastCompletedDates
import com.lsing.timego.domain.routinesForToday
import com.lsing.timego.domain.sessionWorkingSetHistory
import com.lsing.timego.domain.suggestedExerciseFor
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.buildDayHistoryEntries
import com.lsing.timego.ui.common.sessionDayLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val muscleIntensities: Map<String, Float>,
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
    val suggestedExercise: Exercise?,
)

/** One-shot visual acknowledgement emitted only after the repository has saved a set. */
data class SetLoggedPulse(val exerciseId: Long, val eventId: Long)

/** Named holder for the four-way [combine] feeding suggestions/landing balance -- destructured at
 *  the collector, so the positional tuple never escapes this file. */
private data class LandingInputs(
    val exercises: List<Exercise>,
    val setLogs: List<SetLog>,
    val sessions: List<com.lsing.timego.data.WorkoutSession>,
    val timeframe: ProgressTimeframe,
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
    private var exerciseUsageCounts: Map<Long, Int> = emptyMap()
    private var hasAutoSelectedTodaysRoutine = false

    private val _displayedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val displayedExercises: StateFlow<List<Exercise>> = _displayedExercises.asStateFlow()

    private val _lastWorkingSets = MutableStateFlow<Map<Long, SetLog>>(emptyMap())
    val lastWorkingSets: StateFlow<Map<Long, SetLog>> = _lastWorkingSets.asStateFlow()

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

    private val _trainingLean = MutableStateFlow(TrainingLean.BALANCED)

    private val _sessionState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    private val _landingSummary = MutableStateFlow(
        LandingSummary(
            lastSession = null,
            recommendedMuscleGroups = emptyList(),
            suggestedExercise = null,
        ),
    )
    val landingSummary: StateFlow<LandingSummary> = _landingSummary.asStateFlow()

    private val _landingBalanceTimeframe = MutableStateFlow(ProgressTimeframe.MONTH)
    val landingBalanceTimeframe: StateFlow<ProgressTimeframe> = _landingBalanceTimeframe.asStateFlow()

    private val _landingMuscleBalance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val landingMuscleBalance: StateFlow<Map<String, Float>> = _landingMuscleBalance.asStateFlow()

    private val _routineLastCompleted = MutableStateFlow<Map<Long, LocalDate>>(emptyMap())
    val routineLastCompleted: StateFlow<Map<Long, LocalDate>> = _routineLastCompleted.asStateFlow()

    private val _holdDelaySeconds = MutableStateFlow(SettingsRepository.DEFAULT_HOLD_DELAY_SECONDS)
    val holdDelaySeconds: StateFlow<Int> = _holdDelaySeconds.asStateFlow()

    private var nextPulseEventId = 0L
    private val _setLoggedPulse = MutableStateFlow<SetLoggedPulse?>(null)
    val setLoggedPulse: StateFlow<SetLoggedPulse?> = _setLoggedPulse.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.holdDelaySeconds.collect { _holdDelaySeconds.value = it }
        }
        viewModelScope.launch {
            settingsRepository.trainingLean.collect { lean ->
                _trainingLean.value = lean
                refreshLandingSummary()
            }
        }
        viewModelScope.launch {
            repository.seedMissingExercises(SEED_EXERCISES)
            combine(
                repository.exercises,
                repository.setLogs,
                repository.sessions,
                _landingBalanceTimeframe,
            ) { exercises, setLogs, sessions, timeframe ->
                LandingInputs(exercises, setLogs, sessions, timeframe)
            }.collect { (list, setLogs, sessions, timeframe) ->
                allExercises = list
                exerciseUsageCounts = exerciseUsageFrequency(setLogs, list.associateBy { it.id })
                _lastWorkingSets.value = lastWorkingSetByExercise(setLogs, sessions, list.associateBy { it.id })
                // Session state first: refreshSuggestions reads the active session id to decide
                // whether an exercise's suggestion should lock to this session's first working
                // set. Computing it while _sessionState is still Loading made every suggestion
                // fall back to the between-session decision table on a cold start mid-session.
                refreshSessionState()
                refreshSuggestions(list)
                refreshDisplayedExercises()
                _landingMuscleBalance.value = muscleBalanceForTimeframe(
                    timeframe = timeframe,
                    sessions = sessions,
                    sets = setLogs,
                    exercisesById = list.associateBy { it.id },
                    today = LocalDate.now(),
                )
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
            combine(repository.routines, repository.sessions) { _, sessions -> sessions }
                .collect { sessions -> _routineLastCompleted.value = routineLastCompletedDates(sessions) }
        }
        // Collected, not read once: calisthenics sets compute their stored weightKg as
        // bodyweight + added k at log time, so a bodyweight logged on the Progress tab has to
        // reach this screen without a process restart -- otherwise the set is persisted against
        // a stale (or absent) bodyweight and every 1RM/PR derived from it is wrong.
        viewModelScope.launch {
            repository.bodyMetrics.collect { metrics ->
                _latestBodyWeightKg.value = latestWeightKg(metrics)
            }
        }
    }

    fun selectRoutine(routineId: Long?) {
        _selectedRoutineId.value = routineId
        viewModelScope.launch { refreshDisplayedExercises() }
    }

    fun selectLandingBalanceTimeframe(timeframe: ProgressTimeframe) {
        _landingBalanceTimeframe.value = timeframe
    }

    private suspend fun refreshDisplayedExercises() {
        val routineId = _selectedRoutineId.value
        val filteredExercises = if (routineId == null) {
            allExercises
        } else {
            val exerciseIds = repository.exercisesForRoutine(routineId).map { it.exerciseId }.toSet()
            allExercises.filter { it.id in exerciseIds }
        }
        _displayedExercises.value = exercisesRankedByFrequency(filteredExercises, exerciseUsageCounts)
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
                val repRange = repRangeFor(exerciseSets, sessionHistory)
                val historyPerf = sessionHistory.map { SetPerformance(it.weightKg, it.reps, it.targetReps, it.rpe) }
                val currentPerf = currentSessionWorkingSets.map { SetPerformance(it.weightKg, it.reps, it.targetReps, it.rpe) }
                suggester.suggestNext(historyPerf, currentPerf, weightIncrementFor(exercise), repRange)?.let { map[exercise.id] = it }
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
            val muscleGroups = muscleGroupsAffectedInSession(session.id, sets, allExercises)
            val primaryMuscleGroups = muscleGroupsWorkedInSession(session.id, sets, allExercises)
            val muscleIntensities = muscleGroupIntensityForSession(session.id, sets, exercisesById)
            val detail = buildDayHistoryEntries(sets, exercisesById)
            LastSessionSummary(
                sets = sets.size,
                muscleGroups = muscleGroups,
                muscleIntensities = muscleIntensities,
                label = sessionDayLabel(primaryMuscleGroups, isCardioOnlySession(sets, exercisesById)),
                durationMinutes = (session.endEpochMillis ?: session.startEpochMillis).minus(session.startEpochMillis) / 60_000,
                detail = detail,
            )
        }

        val allSets = repository.allSetLogs()
        val sessionDateById = repository.allSessions().associate { it.id to it.date }
        val exercisesById = allExercises.associateBy { it.id }
        val lastTrained = lastTrainedDatesByMuscleGroup(allSets, exercisesById, sessionDateById)
        val allGroups = MuscleGroup.entries.filterNot { it == MuscleGroup.FULL_BODY }.map { it.name }
        val recommendedSeeds = rankUntrainedMuscleGroups(allGroups, lastTrained, LocalDate.now()).take(2)
        val recommended = expandMuscleGroupRegions(recommendedSeeds).toList()
        val suggestedExercise = suggestedExerciseFor(
            targetGroups = recommended.toSet(),
            exercises = allExercises,
            lean = _trainingLean.value,
            usageCounts = exerciseUsageFrequency(allSets, exercisesById),
        )

        _landingSummary.value = LandingSummary(
            lastSession = summary,
            recommendedMuscleGroups = recommended,
            suggestedExercise = suggestedExercise,
        )
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

    fun logSet(
        exerciseId: Long,
        weightKg: Double,
        reps: Int,
        targetReps: Int,
        isWarmup: Boolean = false,
        addedWeightKg: Double? = null,
        rpe: Int? = null,
        targetProvenance: TargetProvenance = TargetProvenance.UNKNOWN,
    ) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logSet(sessionId, exerciseId, weightKg, reps, targetReps, isWarmup, addedWeightKg, rpe, targetProvenance.name)
            refreshSuggestionForExercise(exerciseId)
            emitSetLoggedPulse(exerciseId)
        }
    }

    fun logCardioSet(exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logCardioSet(sessionId, exerciseId, durationMinutes, distanceKm)
            emitSetLoggedPulse(exerciseId)
        }
    }

    fun logHoldSet(
        exerciseId: Long,
        durationSeconds: Int,
        targetDurationSeconds: Int,
        isWarmup: Boolean = false,
        targetProvenance: TargetProvenance = TargetProvenance.UNKNOWN,
    ) {
        val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
        viewModelScope.launch {
            repository.logHoldSet(sessionId, exerciseId, durationSeconds, targetDurationSeconds, isWarmup, targetProvenance.name)
            refreshSuggestionForExercise(exerciseId)
            emitSetLoggedPulse(exerciseId)
        }
    }

    private fun emitSetLoggedPulse(exerciseId: Long) {
        nextPulseEventId += 1
        _setLoggedPulse.value = SetLoggedPulse(exerciseId, nextPulseEventId)
    }

    /** Recomputes the suggestion for just the exercise that was logged, instead of every exercise
     *  in the library (see [refreshSuggestions]'s doc comment for why -- unchanged perf rationale
     *  from the 2026-08-10 logging-field-accuracy session). */
    private suspend fun refreshSuggestionForExercise(exerciseId: Long) {
        val exercise = allExercises.firstOrNull { it.id == exerciseId } ?: return
        val inputs = buildSuggestionInputs(exerciseId)
        val sessionHistory = inputs.sessionHistory
        val currentSessionWorkingSets = inputs.currentSessionWorkingSets
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
            val repRange = repRangeFor(inputs.allWorkingSets, sessionHistory)
            val historyPerf = sessionHistory.map { SetPerformance(it.weightKg, it.reps, it.targetReps, it.rpe) }
            val currentPerf = currentSessionWorkingSets.map { SetPerformance(it.weightKg, it.reps, it.targetReps, it.rpe) }
            val suggestion = suggester.suggestNext(historyPerf, currentPerf, weightIncrementFor(exercise), repRange)
            _suggestions.value = if (suggestion != null) {
                _suggestions.value + (exerciseId to suggestion)
            } else {
                _suggestions.value - exerciseId
            }
        }
    }

    private data class SuggestionInputs(
        val sessionHistory: List<com.lsing.timego.data.SetLog>,
        val currentSessionWorkingSets: List<com.lsing.timego.data.SetLog>,
        val allWorkingSets: List<com.lsing.timego.data.SetLog>,
    )

    /** Shared by [refreshSuggestionForExercise] -- fetches this exercise's full history once,
     *  splits it into past-session representative performances, the active session's own working
     *  sets so far (empty if no session is active, per [SessionUiState]), and the raw non-warmup
     *  set list (for [repRangeFor], which needs every set at a weight, not one per session). */
    private suspend fun buildSuggestionInputs(exerciseId: Long): SuggestionInputs {
        val allSets = repository.historyForExercise(exerciseId)
        val sessionStartById = repository.allSessions().associate { it.id to it.startEpochMillis }
        val sessionHistory = sessionWorkingSetHistory(allSets, sessionStartById)
        val activeSessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId
        val currentSessionWorkingSets = if (activeSessionId != null) {
            allSets.filter { it.sessionId == activeSessionId && !it.isWarmup }.sortedBy { it.loggedAtEpochMillis }
        } else {
            emptyList()
        }
        return SuggestionInputs(sessionHistory, currentSessionWorkingSets, allSets.filterNot { it.isWarmup })
    }

    /** [exerciseSets] must be the raw (unreduced) set list for this exercise -- repRangeAtWeight
     *  needs every set at a given weight, not sessionHistory's one-representative-set-per-session
     *  reduction. Null when there's no working weight yet (sessionHistory empty) or not enough
     *  history at that weight -- both cases correctly fall back to today's behavior in the
     *  suggester. */
    private fun repRangeFor(exerciseSets: List<com.lsing.timego.data.SetLog>, sessionHistory: List<com.lsing.timego.data.SetLog>): RepRange? =
        sessionHistory.lastOrNull()?.let { repRangeAtWeight(exerciseSets, it.weightKg) }

    /** Null for calisthenics: their stored weight is bodyweight + added k, which has no plate
     *  granularity to round to. Everything else steps in [DEFAULT_WEIGHT_INCREMENT_KG]. */
    private fun weightIncrementFor(exercise: Exercise): Double? =
        if (exercise.category == ExerciseCategory.CALISTHENICS.name) null else DEFAULT_WEIGHT_INCREMENT_KG

    fun addCustomExercise(name: String, muscleGroups: List<String>, category: String) {
        viewModelScope.launch {
            repository.addCustomExercise(name, muscleGroups, category)
        }
    }
}
