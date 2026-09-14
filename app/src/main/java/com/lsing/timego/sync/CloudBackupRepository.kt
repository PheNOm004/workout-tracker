package com.lsing.timego.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cloudBackupStore by preferencesDataStore("cloud_backup_consent")

data class CloudBackupConsent(val enabled: Boolean = false, val consentedAtEpochMillis: Long? = null, val consentVersion: Int = 0)

class CloudBackupRepository(private val context: Context) {
    val consent: Flow<CloudBackupConsent> = context.cloudBackupStore.data.map {
        CloudBackupConsent(it[ENABLED] ?: false, it[CONSENTED_AT], if (it[ENABLED] == true) CURRENT_CONSENT_VERSION else 0)
    }

    suspend fun setEnabled(enabled: Boolean, emailVerified: Boolean, backendAvailable: Boolean): Result<Unit> {
        if (enabled && !emailVerified) return Result.failure(IllegalStateException("Verify your email before enabling cloud backup."))
        if (enabled && !backendAvailable) return Result.failure(IllegalStateException("Cloud backup is not configured in this build."))
        context.cloudBackupStore.edit {
            it[ENABLED] = enabled
            if (enabled) it[CONSENTED_AT] = System.currentTimeMillis()
        }
        return Result.success(Unit)
    }

    companion object {
        const val CURRENT_CONSENT_VERSION = 1
        private val ENABLED = booleanPreferencesKey("enabled")
        private val CONSENTED_AT = longPreferencesKey("consented_at_epoch_millis")
    }
}
