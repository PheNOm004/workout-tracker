package com.lsing.timego.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.Routine
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
import com.lsing.timego.domain.untrainedMuscleGroups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class RoutinesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _untrainedGroups = MutableStateFlow<List<String>>(emptyList())
    val untrainedGroups: StateFlow<List<String>> = _untrainedGroups.asStateFlow()

    init {
        viewModelScope.launch { repository.routines.collect { _routines.value = it } }
        viewModelScope.launch {
            repository.exercises.collect { exerciseList ->
                _exercises.value = exerciseList
                refreshUntrainedGroups(exerciseList)
            }
        }
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
}
