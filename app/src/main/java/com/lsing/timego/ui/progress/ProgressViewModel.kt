package com.lsing.timego.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.PersonalRecord
import com.lsing.timego.domain.personalRecords
import com.lsing.timego.domain.strengthCurve
import com.lsing.timego.domain.workoutVolumeRatios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    private val _strengthCurve = MutableStateFlow<List<Pair<LocalDate, Double>>>(emptyList())
    val strengthCurve: StateFlow<List<Pair<LocalDate, Double>>> = _strengthCurve.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessions.collect { sessions ->
                val allSets = repository.allSetLogs()
                _volumeRatios.value = workoutVolumeRatios(sessions, allSets)
                val sessionDateById = sessions.associate { it.id to it.date }
                _records.value = personalRecords(allSets, sessionDateById)
            }
        }
        viewModelScope.launch {
            repository.bodyMetrics.collect { _bodyMetrics.value = it }
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

    fun selectExercise(exerciseId: Long) {
        _selectedExerciseId.value = exerciseId
        viewModelScope.launch {
            val history = repository.historyForExercise(exerciseId)
            val sessionDateById = repository.allSessions().associate { it.id to it.date }
            _strengthCurve.value = strengthCurve(history, sessionDateById)
        }
    }

    fun logBodyMetric(weightKg: Double?, waistCm: Double?) {
        viewModelScope.launch {
            repository.logBodyMetric(LocalDate.now(), weightKg, waistCm)
        }
    }
}
