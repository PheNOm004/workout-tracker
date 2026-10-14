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

enum class CalisthenicsTier { BEGINNER, INTERMEDIATE, ADVANCED }

/** Small app-wide settings, distinct from [TimeGoDatabase] -- a handful of simple preference
 *  values (currently just the hold-exercise start delay) don't need a Room table or migrations. */
class SettingsRepository(private val context: Context) {
    val holdDelaySeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[HOLD_DELAY_SECONDS_KEY] ?: DEFAULT_HOLD_DELAY_SECONDS
    }

    /** Default rest between working sets, in seconds; 0 turns the rest timer off. */
    val restTimerSeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[REST_TIMER_SECONDS_KEY] ?: DEFAULT_REST_TIMER_SECONDS
    }

    /** Adaptive rest adjusts to the program and the last set's RPE; static always uses [restTimerSeconds]. */
    val restAdaptive: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[REST_ADAPTIVE_KEY] ?: true
    }

    /** Exercises whose kg field is typed as both dumbbells combined (stored per hand). */
    val combinedEntryExerciseIds: Flow<Set<Long>> = context.settingsDataStore.data.map { prefs ->
        prefs[COMBINED_ENTRY_IDS_KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet()
    }

    /** Timed holds that run once per side (the timer restarts for the second side). */
    val bothSidesExerciseIds: Flow<Set<Long>> = context.settingsDataStore.data.map { prefs ->
        prefs[BOTH_SIDES_IDS_KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet()
    }

    val trainingLean: Flow<TrainingLean> = context.settingsDataStore.data.map { prefs ->
        prefs[TRAINING_LEAN_KEY]
            ?.let { saved -> runCatching { TrainingLean.valueOf(saved) }.getOrNull() }
            ?: TrainingLean.BALANCED
    }

    val favoriteExerciseIds: Flow<Set<Long>> = context.settingsDataStore.data.map { prefs ->
        prefs[FAVORITE_EXERCISE_IDS_KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet()
    }

    val activeProgramId: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[ACTIVE_PROGRAM_ID_KEY]
    }

    val calisthenicsTier: Flow<CalisthenicsTier> = context.settingsDataStore.data.map { prefs ->
        prefs[CALISTHENICS_TIER_KEY]
            ?.let { saved -> runCatching { CalisthenicsTier.valueOf(saved) }.getOrNull() }
            ?: CalisthenicsTier.BEGINNER
    }

    suspend fun setHoldDelaySeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs -> prefs[HOLD_DELAY_SECONDS_KEY] = seconds }
    }

    suspend fun setRestTimerSeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs -> prefs[REST_TIMER_SECONDS_KEY] = seconds }
    }

    suspend fun setRestAdaptive(adaptive: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[REST_ADAPTIVE_KEY] = adaptive }
    }

    suspend fun toggleCombinedEntry(exerciseId: Long) = toggleId(COMBINED_ENTRY_IDS_KEY, exerciseId)

    suspend fun toggleBothSides(exerciseId: Long) = toggleId(BOTH_SIDES_IDS_KEY, exerciseId)

    private suspend fun toggleId(key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>, exerciseId: Long) {
        context.settingsDataStore.edit { prefs ->
            val saved = prefs[key].orEmpty()
            val id = exerciseId.toString()
            prefs[key] = if (id in saved) saved - id else saved + id
        }
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

    suspend fun setActiveProgramId(id: String?) {
        context.settingsDataStore.edit { prefs ->
            if (id == null) prefs.remove(ACTIVE_PROGRAM_ID_KEY) else prefs[ACTIVE_PROGRAM_ID_KEY] = id
        }
    }

    suspend fun setCalisthenicsTier(tier: CalisthenicsTier) {
        context.settingsDataStore.edit { prefs -> prefs[CALISTHENICS_TIER_KEY] = tier.name }
    }

    companion object {
        const val DEFAULT_HOLD_DELAY_SECONDS = 5
        const val DEFAULT_REST_TIMER_SECONDS = 120
        private val HOLD_DELAY_SECONDS_KEY = intPreferencesKey("hold_delay_seconds")
        private val REST_TIMER_SECONDS_KEY = intPreferencesKey("rest_timer_seconds")
        private val REST_ADAPTIVE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("rest_adaptive")
        private val COMBINED_ENTRY_IDS_KEY = stringSetPreferencesKey("combined_entry_exercise_ids")
        private val BOTH_SIDES_IDS_KEY = stringSetPreferencesKey("both_sides_exercise_ids")
        private val TRAINING_LEAN_KEY = stringPreferencesKey("training_lean")
        private val FAVORITE_EXERCISE_IDS_KEY = stringSetPreferencesKey("favorite_exercise_ids")
        private val ACTIVE_PROGRAM_ID_KEY = stringPreferencesKey("active_program_id")
        private val CALISTHENICS_TIER_KEY = stringPreferencesKey("calisthenics_tier")
    }
}
