package com.lsing.timego.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.PersonalRecord
import com.lsing.timego.domain.TrainingStats
import com.lsing.timego.domain.bodyMassIndex
import com.lsing.timego.domain.isCardioOnlySession
import com.lsing.timego.domain.latestHeightCm
import com.lsing.timego.domain.latestWeightKg
import com.lsing.timego.domain.muscleGroupStrengthCurve
import com.lsing.timego.domain.muscleBalanceForTimeframe
import com.lsing.timego.domain.muscleDistributionForTimeframe
import com.lsing.timego.domain.previousMuscleBalanceForTimeframe
import com.lsing.timego.domain.muscleGroupsWorkedInSession
import com.lsing.timego.domain.personalRecords
import com.lsing.timego.domain.strengthCurve
import com.lsing.timego.domain.trainingStats
import com.lsing.timego.domain.workoutVolumeRatios
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.buildDayHistoryEntries
import com.lsing.timego.ui.common.sessionDayLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CurveMode { EXERCISE, MUSCLE_GROUP }

/** Named holder for the four-way [combine] feeding the Progress screen's derived state --
 *  destructured at the collector, so the positional tuple never escapes this file. */
private data class Inputs(
    val sessions: List<com.lsing.timego.data.WorkoutSession>,
    val allSets: List<com.lsing.timego.data.SetLog>,
    val exercises: List<Exercise>,
    val timeframe: ProgressTimeframe,
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private var latestSessions: List<com.lsing.timego.data.WorkoutSession> = emptyList()
    private var latestSetLogs: List<com.lsing.timego.data.SetLog> = emptyList()
    private var latestExercises: List<Exercise> = emptyList()

    private val _volumeRatios = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    val volumeRatios: StateFlow<Map<LocalDate, Float>> = _volumeRatios.asStateFlow()

    private val _records = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val records: StateFlow<List<PersonalRecord>> = _records.asStateFlow()

    private val _bodyMetrics = MutableStateFlow<List<BodyMetric>>(emptyList())
    val bodyMetrics: StateFlow<List<BodyMetric>> = _bodyMetrics.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    private val _curveMode = MutableStateFlow(CurveMode.EXERCISE)
    val curveMode: StateFlow<CurveMode> = _curveMode.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup.asStateFlow()

    private val _strengthCurve = MutableStateFlow<List<Pair<LocalDate, Double>>>(emptyList())
    val strengthCurve: StateFlow<List<Pair<LocalDate, Double>>> = _strengthCurve.asStateFlow()

    private val _selectedHistoryDate = MutableStateFlow<LocalDate?>(null)
    val selectedHistoryDate: StateFlow<LocalDate?> = _selectedHistoryDate.asStateFlow()

    private val _historyForSelectedDate = MutableStateFlow<List<DayHistoryEntry>>(emptyList())
    val historyForSelectedDate: StateFlow<List<DayHistoryEntry>> = _historyForSelectedDate.asStateFlow()

    private val _historyLabel = MutableStateFlow<String?>(null)
    val historyLabel: StateFlow<String?> = _historyLabel.asStateFlow()

    private val _weightCurve = MutableStateFlow<List<Pair<LocalDate, Double>>>(emptyList())
    val weightCurve: StateFlow<List<Pair<LocalDate, Double>>> = _weightCurve.asStateFlow()

    private val _currentBmi = MutableStateFlow<Double?>(null)
    val currentBmi: StateFlow<Double?> = _currentBmi.asStateFlow()

    private val _muscleDistribution = MutableStateFlow<Map<String, Float>>(emptyMap())
    val muscleDistribution: StateFlow<Map<String, Float>> = _muscleDistribution.asStateFlow()

    private val _muscleBalance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val muscleBalance: StateFlow<Map<String, Float>> = _muscleBalance.asStateFlow()

    private val _previousMuscleBalance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val previousMuscleBalance: StateFlow<Map<String, Float>> = _previousMuscleBalance.asStateFlow()

    private val _trainingStats = MutableStateFlow(TrainingStats(0, 0.0, 0.0, 0))
    val trainingStats: StateFlow<TrainingStats> = _trainingStats.asStateFlow()

    private val _timeframe = MutableStateFlow(ProgressTimeframe.MONTH)
    val timeframe: StateFlow<ProgressTimeframe> = _timeframe.asStateFlow()

    init {
        // Every input this block reads is now an observed Flow. Previously only `sessions` and
        // `_timeframe` drove it while the set list was fetched one-shot inside, so sets logged
        // into an already-open session never refreshed records/volume/stats/distribution.
        viewModelScope.launch {
            combine(
                repository.sessions,
                repository.setLogs,
                repository.exercises,
                _timeframe,
            ) { sessions, allSets, exerciseList, timeframe ->
                Inputs(sessions, allSets, exerciseList, timeframe)
            }.collect { (sessions, allSets, exerciseList, timeframe) ->
                latestSessions = sessions
                latestSetLogs = allSets
                latestExercises = exerciseList
                _exercises.value = exerciseList
                if (_selectedExerciseId.value == null) {
                    _selectedExerciseId.value = exerciseList.firstOrNull()?.id
                }
                val sessionDateById = sessions.associate { it.id to it.date }
                val exercisesById = exerciseList.associateBy { it.id }
                val today = LocalDate.now()

                _volumeRatios.value = workoutVolumeRatios(sessions, allSets)
                _records.value = personalRecords(allSets, sessionDateById, exercisesById)
                _trainingStats.value = trainingStats(
                    sessions,
                    allSets,
                    timeframe.sinceDate(sessions.minOfOrNull { it.date }, today),
                )
                _muscleDistribution.value = muscleDistributionForTimeframe(
                    timeframe = timeframe,
                    sessions = sessions,
                    sets = allSets,
                    exercisesById = exercisesById,
                    today = today,
                )
                _muscleBalance.value = muscleBalanceForTimeframe(
                    timeframe = timeframe,
                    sessions = sessions,
                    sets = allSets,
                    exercisesById = exercisesById,
                    today = today,
                )
                _previousMuscleBalance.value = previousMuscleBalanceForTimeframe(
                    timeframe = timeframe,
                    sessions = sessions,
                    sets = allSets,
                    exercisesById = exercisesById,
                    today = today,
                )
                refreshStrengthCurve()
                refreshSelectedHistory()
            }
        }
        viewModelScope.launch {
            repository.bodyMetrics.collect { metrics ->
                _bodyMetrics.value = metrics
                _weightCurve.value = metrics.mapNotNull { metric -> metric.weightKg?.let { metric.date to it } }
                val latestWeight = latestWeightKg(metrics)
                val latestHeight = latestHeightCm(metrics)
                _currentBmi.value = if (latestWeight != null && latestHeight != null) {
                    bodyMassIndex(latestWeight, latestHeight)
                } else {
                    null
                }
            }
        }
    }

    fun selectTimeframe(timeframe: ProgressTimeframe) {
        _timeframe.value = timeframe
    }

    fun selectExercise(exerciseId: Long) {
        _curveMode.value = CurveMode.EXERCISE
        _selectedExerciseId.value = exerciseId
        refreshStrengthCurve()
    }

    fun selectMuscleGroup(group: String) {
        _curveMode.value = CurveMode.MUSCLE_GROUP
        _selectedMuscleGroup.value = group
        refreshStrengthCurve()
    }

    fun logBodyMetric(weightKg: Double?, waistCm: Double?, heightCm: Double?) {
        viewModelScope.launch {
            repository.logBodyMetric(LocalDate.now(), weightKg, waistCm, heightCm)
        }
    }

    /** Populates [historyForSelectedDate] with every set logged on [date], across all sessions
     *  that day (freeform and routine sessions are both just WorkoutSessions, so no special
     *  casing needed) -- backs the heatmap's tap-to-see-workout-history feature. Pass null to
     *  dismiss the detail view. */
    fun selectHistoryDate(date: LocalDate?) {
        _selectedHistoryDate.value = date
        if (date == null) {
            _historyForSelectedDate.value = emptyList()
            _historyLabel.value = null
            return
        }
        refreshSelectedHistory()
    }

    private fun refreshStrengthCurve() {
        val sessionDateById = latestSessions.associate { it.id to it.date }
        _strengthCurve.value = when (_curveMode.value) {
            CurveMode.EXERCISE -> {
                val exerciseId = _selectedExerciseId.value ?: return
                strengthCurve(latestSetLogs.filter { it.exerciseId == exerciseId }, sessionDateById)
            }
            CurveMode.MUSCLE_GROUP -> {
                val group = _selectedMuscleGroup.value ?: return
                muscleGroupStrengthCurve(
                    latestSetLogs,
                    latestExercises.associateBy { it.id },
                    sessionDateById,
                    group,
                )
            }
        }
    }

    private fun refreshSelectedHistory() {
        val date = _selectedHistoryDate.value ?: return
        val sessionIds = latestSessions.filter { it.date == date }.map { it.id }.toSet()
        val exercisesById = latestExercises.associateBy { it.id }
        val sets = latestSetLogs.filter { it.sessionId in sessionIds }.sortedBy { it.loggedAtEpochMillis }
        _historyForSelectedDate.value = buildDayHistoryEntries(sets, exercisesById)
        val primaryMuscleGroups = sessionIds.flatMap { sessionId ->
            muscleGroupsWorkedInSession(sessionId, sets, latestExercises)
        }.toSet()
        _historyLabel.value = sessionDayLabel(primaryMuscleGroups, isCardioOnlySession(sets, exercisesById))
    }
}
