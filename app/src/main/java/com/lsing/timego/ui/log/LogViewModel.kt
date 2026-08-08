package com.lsing.timego.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SEED_EXERCISES
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.OverloadSuggestion
import com.lsing.timego.domain.RuleBasedOverloadSuggester
import com.lsing.timego.domain.SetPerformance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [selectedRoutineId] null means freeform (all exercises shown, sessions logged with no routine
 *  link); non-null filters [displayedExercises] to that routine's exercises and tags logged
 *  sessions with it -- this is what makes a session "routine-based" vs. "freeform" per the spec. */
class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private val suggester = RuleBasedOverloadSuggester()

    private var allExercises: List<Exercise> = emptyList()

    private val _displayedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val displayedExercises: StateFlow<List<Exercise>> = _displayedExercises.asStateFlow()

    private val _suggestions = MutableStateFlow<Map<Long, OverloadSuggestion>>(emptyMap())
    val suggestions: StateFlow<Map<Long, OverloadSuggestion>> = _suggestions.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _selectedRoutineId = MutableStateFlow<Long?>(null)
    val selectedRoutineId: StateFlow<Long?> = _selectedRoutineId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedExercisesIfEmpty(SEED_EXERCISES)
            repository.exercises.collect { list ->
                allExercises = list
                refreshSuggestions(list)
                refreshDisplayedExercises()
            }
        }
        viewModelScope.launch {
            repository.routines.collect { _routines.value = it }
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

    private suspend fun refreshSuggestions(exerciseList: List<Exercise>) {
        val map = mutableMapOf<Long, OverloadSuggestion>()
        for (exercise in exerciseList) {
            val history = repository.historyForExercise(exercise.id)
                .map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            suggester.suggestNext(history)?.let { map[exercise.id] = it }
        }
        _suggestions.value = map
    }

    fun logSet(exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int) {
        viewModelScope.launch {
            val session = repository.startOrGetTodaySession(routineId = _selectedRoutineId.value)
            repository.logSet(session.id, exerciseId, weightKg, reps, targetReps)
            refreshSuggestions(allExercises)
        }
    }

    fun addCustomExercise(name: String, muscleGroups: List<String>) {
        viewModelScope.launch {
            repository.addCustomExercise(name, muscleGroups)
        }
    }
}
