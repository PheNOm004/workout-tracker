package com.lsing.timego.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Small app-wide settings, distinct from [TimeGoDatabase] -- a handful of simple preference
 *  values (currently just the hold-exercise start delay) don't need a Room table or migrations. */
class SettingsRepository(private val context: Context) {
    val holdDelaySeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[HOLD_DELAY_SECONDS_KEY] ?: DEFAULT_HOLD_DELAY_SECONDS
    }

    suspend fun setHoldDelaySeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs -> prefs[HOLD_DELAY_SECONDS_KEY] = seconds }
    }

    companion object {
        const val DEFAULT_HOLD_DELAY_SECONDS = 5
        private val HOLD_DELAY_SECONDS_KEY = intPreferencesKey("hold_delay_seconds")
    }
}
