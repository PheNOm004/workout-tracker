package com.lsing.timego.report

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.DateTimeException
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reportSubscriptionStore by preferencesDataStore("report_subscriptions")

enum class ReportCadence { WEEKLY, MONTHLY }

data class ReportSubscription(
    val weeklyEnabled: Boolean = false,
    val monthlyEnabled: Boolean = false,
    val timezoneId: String = ZoneId.systemDefault().id,
    val consentVersion: Int = 0,
    val consentedAtEpochMillis: Long? = null,
)

enum class SubscriptionFailure { EMAIL_NOT_VERIFIED, CLOUD_BACKUP_REQUIRED, INVALID_TIMEZONE }

sealed interface SubscriptionResult {
    data object Success : SubscriptionResult
    data class Failure(val reason: SubscriptionFailure) : SubscriptionResult
}

class ReportSubscriptionRepository(private val context: Context) {
    val subscription: Flow<ReportSubscription> = context.reportSubscriptionStore.data.map { preferences ->
        ReportSubscription(
            weeklyEnabled = preferences[WEEKLY] ?: false,
            monthlyEnabled = preferences[MONTHLY] ?: false,
            timezoneId = preferences[TIMEZONE] ?: ZoneId.systemDefault().id,
            consentVersion = preferences[CONSENT_VERSION] ?: 0,
            consentedAtEpochMillis = preferences[CONSENTED_AT],
        )
    }

    suspend fun setCadence(
        cadence: ReportCadence,
        enabled: Boolean,
        emailVerified: Boolean,
        cloudBackupEnabled: Boolean,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): SubscriptionResult {
        if (enabled && !emailVerified) return SubscriptionResult.Failure(SubscriptionFailure.EMAIL_NOT_VERIFIED)
        if (enabled && !cloudBackupEnabled) return SubscriptionResult.Failure(SubscriptionFailure.CLOUD_BACKUP_REQUIRED)
        context.reportSubscriptionStore.edit { preferences ->
            preferences[if (cadence == ReportCadence.WEEKLY) WEEKLY else MONTHLY] = enabled
            preferences[CONSENT_VERSION] = CURRENT_CONSENT_VERSION
            preferences[CONSENTED_AT] = nowEpochMillis
        }
        return SubscriptionResult.Success
    }

    suspend fun setTimezone(timezoneId: String): SubscriptionResult {
        try { ZoneId.of(timezoneId) } catch (_: DateTimeException) { return SubscriptionResult.Failure(SubscriptionFailure.INVALID_TIMEZONE) }
        context.reportSubscriptionStore.edit { it[TIMEZONE] = timezoneId }
        return SubscriptionResult.Success
    }

    suspend fun unsubscribeAll() {
        context.reportSubscriptionStore.edit {
            it[WEEKLY] = false
            it[MONTHLY] = false
        }
    }

    companion object {
        const val CURRENT_CONSENT_VERSION = 1
        private val WEEKLY = booleanPreferencesKey("weekly_enabled")
        private val MONTHLY = booleanPreferencesKey("monthly_enabled")
        private val TIMEZONE = stringPreferencesKey("timezone_id")
        private val CONSENT_VERSION = intPreferencesKey("consent_version")
        private val CONSENTED_AT = longPreferencesKey("consented_at_epoch_millis")
    }
}
