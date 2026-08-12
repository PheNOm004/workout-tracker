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
import com.lsing.timego.domain.muscleGroupStrengthCurve
import com.lsing.timego.domain.muscleGroupVolumeDistribution
import com.lsing.timego.domain.personalRecords
import com.lsing.timego.domain.strengthCurve
import com.lsing.timego.domain.trainingStats
import com.lsing.timego.domain.workoutVolumeRatios
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.buildDayHistoryEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CurveMode { EXERCISE, MUSCLE_GROUP }

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))

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

    private val _weightCurve = MutableStateFlow<List<Pair<LocalDate, Double>>>(emptyList())
    val weightCurve: StateFlow<List<Pair<LocalDate, Double>>> = _weightCurve.asStateFlow()

    private val _currentBmi = MutableStateFlow<Double?>(null)
    val currentBmi: StateFlow<Double?> = _currentBmi.asStateFlow()

    private val _muscleDistribution = MutableStateFlow<Map<String, Float>>(emptyMap())
    val muscleDistribution: StateFlow<Map<String, Float>> = _muscleDistribution.asStateFlow()

    private val _trainingStats = MutableStateFlow(TrainingStats(0, 0.0, 0.0, 0))
    val trainingStats: StateFlow<TrainingStats> = _trainingStats.asStateFlow()

    private val _timeframe = MutableStateFlow(ProgressTimeframe.MONTH)
    val timeframe: StateFlow<ProgressTimeframe> = _timeframe.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.sessions, _timeframe) { sessions, timeframe -> sessions to timeframe }
                .collect { (sessions, timeframe) ->
                    val allSets = repository.allSetLogs()
                    _volumeRatios.value = workoutVolumeRatios(sessions, allSets)
                    val sessionDateById = sessions.associate { it.id to it.date }
                    val exercisesById = repository.exercises.first().associateBy { it.id }
                    _records.value = personalRecords(allSets, sessionDateById, exercisesById)

                    val earliestSessionDate = sessions.minOfOrNull { it.date }
                    val since = timeframe.sinceDate(earliestSessionDate, LocalDate.now())
                    _trainingStats.value = trainingStats(sessions, allSets, since)
                    val rawDistribution = muscleGroupVolumeDistribution(allSets, exercisesById, sessionDateById, since)
                    val maxVolume = rawDistribution.values.maxOrNull() ?: 0.0
                    _muscleDistribution.value = if (maxVolume > 0.0) {
                        rawDistribution.mapValues { (_, volume) -> (volume / maxVolume).toFloat() }
                    } else {
                        emptyMap()
                    }
                }
        }
        viewModelScope.launch {
            repository.bodyMetrics.collect { metrics ->
                _bodyMetrics.value = metrics
                _weightCurve.value = metrics.mapNotNull { metric -> metric.weightKg?.let { metric.date to it } }
                val latestWeight = metrics.lastOrNull { it.weightKg != null }?.weightKg
                val latestHeight = metrics.lastOrNull { it.heightCm != null }?.heightCm
                _currentBmi.value = if (latestWeight != null && latestHeight != null) {
                    bodyMassIndex(latestWeight, latestHeight)
                } else {
                    null
                }
            }
        }
        viewModelScope.launch {
            repository.exercises.collect { exerciseList ->
                _exercises.value = exerciseList
                if (_selectedExerciseId.value == null) {
                    exerciseList.firstOrNull()?.let { selectExercise(it.id) }
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
        viewModelScope.launch {
            val history = repository.historyForExercise(exerciseId)
            val sessionDateById = repository.allSessions().associate { it.id to it.date }
            _strengthCurve.value = strengthCurve(history, sessionDateById)
        }
    }

    fun selectMuscleGroup(group: String) {
        _curveMode.value = CurveMode.MUSCLE_GROUP
        _selectedMuscleGroup.value = group
        viewModelScope.launch {
            val allSets = repository.allSetLogs()
            val exercisesById = _exercises.value.associateBy { it.id }
            val sessionDateById = repository.allSessions().associate { it.id to it.date }
            _strengthCurve.value = muscleGroupStrengthCurve(allSets, exercisesById, sessionDateById, group)
        }
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
            return
        }
        viewModelScope.launch {
            val sessionIds = repository.allSessions().filter { it.date == date }.map { it.id }.toSet()
            val exercisesById = _exercises.value.associateBy { it.id }
            val sets = repository.allSetLogs().filter { it.sessionId in sessionIds }.sortedBy { it.loggedAtEpochMillis }
            _historyForSelectedDate.value = buildDayHistoryEntries(sets, exercisesById)
        }
    }
}
