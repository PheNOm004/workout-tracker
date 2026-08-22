package com.lsing.timego.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class TrainingLean { STRENGTH, BALANCED, CALISTHENICS }

/** Small app-wide settings, distinct from [TimeGoDatabase] -- a handful of simple preference
 *  values (currently just the hold-exercise start delay) don't need a Room table or migrations. */
class SettingsRepository(private val context: Context) {
    val holdDelaySeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[HOLD_DELAY_SECONDS_KEY] ?: DEFAULT_HOLD_DELAY_SECONDS
    }

    val trainingLean: Flow<TrainingLean> = context.settingsDataStore.data.map { prefs ->
        prefs[TRAINING_LEAN_KEY]
            ?.let { saved -> runCatching { TrainingLean.valueOf(saved) }.getOrNull() }
            ?: TrainingLean.BALANCED
    }

    val favoriteExerciseIds: Flow<Set<Long>> = context.settingsDataStore.data.map { prefs ->
        prefs[FAVORITE_EXERCISE_IDS_KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet()
    }

    suspend fun setHoldDelaySeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs -> prefs[HOLD_DELAY_SECONDS_KEY] = seconds }
    }

    suspend fun setTrainingLean(lean: TrainingLean) {
        context.settingsDataStore.edit { prefs -> prefs[TRAINING_LEAN_KEY] = lean.name }
    }

    suspend fun toggleFavoriteExercise(exerciseId: Long) {
        context.settingsDataStore.edit { prefs ->
            val savedIds = prefs[FAVORITE_EXERCISE_IDS_KEY].orEmpty()
            val id = exerciseId.toString()
            prefs[FAVORITE_EXERCISE_IDS_KEY] = if (id in savedIds) savedIds - id else savedIds + id
        }
    }

    companion object {
        const val DEFAULT_HOLD_DELAY_SECONDS = 5
        private val HOLD_DELAY_SECONDS_KEY = intPreferencesKey("hold_delay_seconds")
        private val TRAINING_LEAN_KEY = stringPreferencesKey("training_lean")
        private val FAVORITE_EXERCISE_IDS_KEY = stringSetPreferencesKey("favorite_exercise_ids")
    }
}
