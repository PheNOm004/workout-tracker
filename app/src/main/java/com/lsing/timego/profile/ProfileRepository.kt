package com.lsing.timego.profile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lsing.timego.data.TrainingLean
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.trainingProfileDataStore by preferencesDataStore(name = "training_profile")

class ProfileRepository(private val context: Context) {
    val profile: Flow<TrainingProfile> = context.trainingProfileDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)

    suspend fun save(profile: TrainingProfile) {
        require(profile.hasValidPhysicalValues) { "Physical values are outside supported bounds" }
        context.trainingProfileDataStore.edit { preferences ->
            preferences.putOrRemove(DISPLAY_NAME, profile.displayName?.trim()?.takeIf(String::isNotEmpty))
            preferences.putOrRemove(AGE_RANGE, profile.ageRange?.name)
            preferences.putOrRemove(HEIGHT_CM, profile.heightCm)
            preferences.putOrRemove(WEIGHT_KG, profile.weightKg)
            preferences.putOrRemove(EXPERIENCE, profile.experience?.name)
            preferences.putOrRemove(GOAL, profile.goal?.name)
            preferences.putOrRemove(MODALITY, profile.modality?.name)
            preferences[EQUIPMENT] = profile.equipment.map(Equipment::name).toSet()
            preferences.putOrRemove(TRAINING_DAYS, profile.trainingDaysPerWeek)
            preferences.putOrRemove(SESSION_DURATION, profile.sessionDuration?.name)
            preferences[MOVEMENT_LIMITATIONS] = profile.movementLimitations.map(MovementLimitation::name).toSet()
            preferences.putOrRemove(PRIVATE_LIMITATION_NOTE, profile.privateLimitationNote?.trim()?.takeIf(String::isNotEmpty))
            preferences[ONBOARDING_VERSION] = profile.onboardingVersion
            preferences[INVITATION_DISMISSED] = profile.invitationDismissed
        }
    }

    suspend fun reset() {
        context.trainingProfileDataStore.edit { it.clear() }
    }

    private fun decode(preferences: Preferences): TrainingProfile = TrainingProfile(
        displayName = preferences[DISPLAY_NAME],
        ageRange = preferences[AGE_RANGE].asEnumOrNull<AgeRange>(),
        heightCm = preferences[HEIGHT_CM],
        weightKg = preferences[WEIGHT_KG],
        experience = preferences[EXPERIENCE].asEnumOrNull<ExperienceLevel>(),
        goal = preferences[GOAL].asEnumOrNull<TrainingGoal>(),
        modality = preferences[MODALITY].asEnumOrNull<TrainingLean>(),
        equipment = preferences[EQUIPMENT].orEmpty().mapNotNull { it.asEnumOrNull<Equipment>() }.toSet(),
        trainingDaysPerWeek = preferences[TRAINING_DAYS],
        sessionDuration = preferences[SESSION_DURATION].asEnumOrNull<SessionDurationRange>(),
        movementLimitations = preferences[MOVEMENT_LIMITATIONS].orEmpty()
            .mapNotNull { it.asEnumOrNull<MovementLimitation>() }
            .toSet(),
        privateLimitationNote = preferences[PRIVATE_LIMITATION_NOTE],
        onboardingVersion = preferences[ONBOARDING_VERSION] ?: 0,
        invitationDismissed = preferences[INVITATION_DISMISSED] ?: false,
    )

    companion object {
        const val CURRENT_ONBOARDING_VERSION = 1
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val AGE_RANGE = stringPreferencesKey("age_range")
        private val HEIGHT_CM = doublePreferencesKey("height_cm")
        private val WEIGHT_KG = doublePreferencesKey("weight_kg")
        private val EXPERIENCE = stringPreferencesKey("experience")
        private val GOAL = stringPreferencesKey("goal")
        private val MODALITY = stringPreferencesKey("modality")
        private val EQUIPMENT = stringSetPreferencesKey("equipment")
        private val TRAINING_DAYS = intPreferencesKey("training_days")
        private val SESSION_DURATION = stringPreferencesKey("session_duration")
        private val MOVEMENT_LIMITATIONS = stringSetPreferencesKey("movement_limitations")
        private val PRIVATE_LIMITATION_NOTE = stringPreferencesKey("private_limitation_note")
        private val ONBOARDING_VERSION = intPreferencesKey("onboarding_version")
        private val INVITATION_DISMISSED = booleanPreferencesKey("invitation_dismissed")
    }
}

private inline fun <reified T : Enum<T>> String?.asEnumOrNull(): T? =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value == null) remove(key) else this[key] = value
}
