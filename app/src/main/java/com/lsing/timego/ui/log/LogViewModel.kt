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
import com.lsing.timego.domain.quickAddExercises
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
import com.lsing.timego.domain.recommendSynergisticMuscleGroups
import com.lsing.timego.domain.repRangeAtWeight
import com.lsing.timego.domain.routineLastCompletedDates
import com.lsing.timego.domain.routinesForToday
import com.lsing.timego.domain.sessionWorkingSetHistory
import com.lsing.timego.domain.suggestedExerciseFor
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.buildDayHistoryEntries
import com.lsing.timego.ui.common.sessionDayLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var latestSetLogs: List<SetLog> = emptyList()
    private var latestSessions: List<com.lsing.timego.data.WorkoutSession> = emptyList()
    private var exerciseUsageCounts: Map<Long, Int> = emptyMap()
    private var hasAutoSelectedTodaysRoutine = false

    private val _displayedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val displayedExercises: StateFlow<List<Exercise>> = _displayedExercises.asStateFlow()

    private val _quickAddExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val quickAddExercises: StateFlow<List<Exercise>> = _quickAddExercises.asStateFlow()

    private val _favoriteExerciseIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteExerciseIds: StateFlow<Set<Long>> = _favoriteExerciseIds.asStateFlow()

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

    private val _activeSessionSets = MutableStateFlow<List<SetLog>>(emptyList())
    val activeSessionSets: StateFlow<List<SetLog>> = _activeSessionSets.asStateFlow()

    private val _activeSessionSetsByExercise = MutableStateFlow<Map<Long, List<SetLog>>>(emptyMap())
    val activeSessionSetsByExercise: StateFlow<Map<Long, List<SetLog>>> = _activeSessionSetsByExercise.asStateFlow()

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
            repository.seedMissingExercises(SEED_EXERCISES)
            // This ViewModel is activity-scoped by the custom root-tab host. Its session state is
            // always collected while Log is STARTED, so subscriber presence is the lifecycle
            // signal that starts all Room/DataStore work and cancels it off-screen/backgrounded.
            _sessionState.subscriptionCount
                .map { count -> count > 0 }
                .distinctUntilChanged()
                .collectLatest { screenStarted ->
                    if (!screenStarted) return@collectLatest
                    coroutineScope {
                        launch {
                            settingsRepository.holdDelaySeconds.collect { _holdDelaySeconds.value = it }
                        }
                        launch {
                            settingsRepository.favoriteExerciseIds.collect { _favoriteExerciseIds.value = it }
                        }
                        launch {
                            settingsRepository.trainingLean.collect { lean ->
                                _trainingLean.value = lean
                                refreshLandingSummary(allExercises, latestSetLogs, latestSessions)
                            }
                        }
                        launch {
                            combine(
                                repository.exercises,
                                repository.setLogs,
                                repository.sessions,
                                _landingBalanceTimeframe,
                            ) { exercises, setLogs, sessions, timeframe ->
                                LandingInputs(exercises, setLogs, sessions, timeframe)
                            }.collect { (list, setLogs, sessions, timeframe) ->
                                allExercises = list
                                latestSetLogs = setLogs
                                latestSessions = sessions
                                _routineLastCompleted.value = routineLastCompletedDates(sessions)
                                val exercisesById = list.associateBy { it.id }
                                // One full-history usage-frequency pass per emission, shared by
                                // session state (landing summary) and the picker ranks below --
                                // refreshLandingSummary used to repeat this same pass internally.
                                val usageCounts = withContext(Dispatchers.Default) {
                                    exerciseUsageFrequency(setLogs, exercisesById)
                                }
                                exerciseUsageCounts = usageCounts
                                // Put the saved set on screen before rebuilding whole-history
                                // suggestions, recommendations, and balance data.
                                refreshSessionState(sessions, setLogs, list, usageCounts)
                                _lastWorkingSets.value = withContext(Dispatchers.Default) {
                                    lastWorkingSetByExercise(setLogs, sessions, exercisesById)
                                }
                                // Suggestions read active session state so later working sets stay
                                // locked to the first set's weight/target for this session.
                                refreshSuggestions(list, setLogs, sessions)
                                refreshDisplayedExercises()
                                _landingMuscleBalance.value = withContext(Dispatchers.Default) {
                                    muscleBalanceForTimeframe(
                                        timeframe = timeframe,
                                        sessions = sessions,
                                        sets = setLogs,
                                        exercisesById = exercisesById,
                                        today = LocalDate.now(),
                                    )
                                }
                            }
                        }
                        launch {
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
                        // Collected, not read once: calisthenics sets compute stored weightKg as
                        // bodyweight + added k, so a Progress update must reach Log without restart.
                        launch {
                            repository.bodyMetrics.collect { metrics ->
                                _latestBodyWeightKg.value = latestWeightKg(metrics)
                            }
                        }
                    }
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
        val rankedExercises = exercisesRankedByFrequency(filteredExercises, exerciseUsageCounts)
        _displayedExercises.value = rankedExercises
        _quickAddExercises.value = quickAddExercises(rankedExercises, exerciseUsageCounts)
    }

    /** Splits suggestion computation by loggingType: WEIGHT_REPS exercises get a weight/reps
     *  suggestion from [suggester], HOLD exercises get a duration suggestion from [holdSuggester] --
     *  an exercise can only produce one kind, so each history is built from the fields that are real
     *  for that exercise (see SetLog's doc comment on its sentinel-field convention). Each exercise's
     *  raw sets are reduced to [sessionWorkingSetHistory] (one representative set per past session)
     *  plus, separately, the active session's own working sets for that exercise so far -- see the
     *  2026-08-12 warmup-session-aware-suggester design for why suggestions no longer look at a flat
     *  raw-set history. */
    private suspend fun refreshSuggestions(
        exerciseList: List<Exercise>,
        allSets: List<SetLog>,
        sessions: List<com.lsing.timego.data.WorkoutSession>,
    ) {
        val activeSessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId
        val (suggestionMap, holdSuggestionMap) = withContext(Dispatchers.Default) {
            val sessionStartById = sessions.associate { it.id to it.startEpochMillis }
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
            map to holdMap
        }
        _suggestions.value = suggestionMap
        _holdSuggestions.value = holdSuggestionMap
    }

    private suspend fun refreshSessionState(
        sessions: List<com.lsing.timego.data.WorkoutSession>,
        allSets: List<SetLog>,
        exercises: List<Exercise>,
        usageCounts: Map<Long, Int> = exerciseUsageCounts,
    ) {
        val active = sessions.firstOrNull { it.endEpochMillis == null }
        if (active != null) {
            val sets = allSets.filter { it.sessionId == active.id }.sortedBy { it.loggedAtEpochMillis }
            _activeSessionSets.value = sets
            _activeSessionSetsByExercise.value = sets.groupBy { it.exerciseId }
            val lastSetTime = sets.maxOfOrNull { it.loggedAtEpochMillis }
            val decision = checkSessionAutoClose(lastSetTime, active.startEpochMillis, System.currentTimeMillis())
            if (decision == SessionAutoCloseDecision.AUTO_CLOSE) {
                val endTime = lastSetTime ?: active.startEpochMillis
                repository.endSession(active.id, endTime)
                latestSessions = sessions.map { session ->
                    if (session.id == active.id) session.copy(endEpochMillis = endTime) else session
                }
                refreshLandingSummary(exercises, allSets, latestSessions, usageCounts)
                _activeSessionSets.value = emptyList()
                _activeSessionSetsByExercise.value = emptyMap()
                _sessionState.value = SessionUiState.NoActiveSession
                return
            }
            _sessionState.value = SessionUiState.Active(active.id)
        } else {
            _activeSessionSets.value = emptyList()
            _activeSessionSetsByExercise.value = emptyMap()
            _sessionState.value = SessionUiState.NoActiveSession
        }
        // Publish the active-session UI first. The landing summary is derived in the background
        // so saving a set never waits for recommendation/history work before the row can update.
        refreshLandingSummary(exercises, allSets, sessions, usageCounts)
    }

    private suspend fun refreshLandingSummary(
        exercises: List<Exercise>,
        allSets: List<SetLog>,
        sessions: List<com.lsing.timego.data.WorkoutSession>,
        usageCounts: Map<Long, Int> = exerciseUsageCounts,
    ) {
        val trainingLean = _trainingLean.value
        val landingSummary = withContext(Dispatchers.Default) {
            val lastSession = sessions
                .asSequence()
                .filter { it.endEpochMillis != null }
                .maxByOrNull { it.endEpochMillis!! }
            val summary = lastSession?.let { session ->
                val sets = allSets.filter { it.sessionId == session.id }
                val exercisesById = exercises.associateBy { it.id }
                val muscleGroups = muscleGroupsAffectedInSession(session.id, sets, exercises)
                val primaryMuscleGroups = muscleGroupsWorkedInSession(session.id, sets, exercises)
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

            val sessionDateById = sessions.associate { it.id to it.date }
            val exercisesById = exercises.associateBy { it.id }
            val lastTrained = lastTrainedDatesByMuscleGroup(allSets, exercisesById, sessionDateById)
            val allGroups = MuscleGroup.entries.filterNot { it == MuscleGroup.FULL_BODY }.map { it.name }
            val recommendedSeeds = recommendSynergisticMuscleGroups(allGroups, lastTrained, LocalDate.now())
            val recommended = expandMuscleGroupRegions(recommendedSeeds).toList()
            val suggestedExercise = suggestedExerciseFor(
                targetGroups = recommended.toSet(),
                exercises = exercises,
                lean = trainingLean,
                usageCounts = usageCounts,
            )

            LandingSummary(
                lastSession = summary,
                recommendedMuscleGroups = recommended,
                suggestedExercise = suggestedExercise,
            )
        }
        _landingSummary.value = landingSummary
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
            val endedAt = System.currentTimeMillis()
            repository.endSession(current.sessionId, endedAt)
            latestSessions = latestSessions.map { session ->
                if (session.id == current.sessionId) session.copy(endEpochMillis = endedAt) else session
            }
            refreshLandingSummary(allExercises, latestSetLogs, latestSessions)
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
            emitSetLoggedPulse(exerciseId)
        }
    }

    fun toggleFavoriteExercise(exerciseId: Long) {
        viewModelScope.launch { settingsRepository.toggleFavoriteExercise(exerciseId) }
    }

    private fun emitSetLoggedPulse(exerciseId: Long) {
        nextPulseEventId += 1
        _setLoggedPulse.value = SetLoggedPulse(exerciseId, nextPulseEventId)
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
