package com.lsing.timego.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.lsing.timego.data.TimeGoDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lsing.timego.report.ReportSubscriptionRepository

class CloudBackupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CloudBackupRepository(application)
    val consent = repository.consent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CloudBackupConsent())
    val pendingCount = TimeGoDatabase.getInstance(application).syncDao().observePendingCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val backendAvailable = FirebaseApp.getApps(application).isNotEmpty()
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage.asStateFlow()

    fun setEnabled(enabled: Boolean, emailVerified: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(enabled, emailVerified, backendAvailable).fold(
                onSuccess = {
                    if (!enabled) ReportSubscriptionRepository(getApplication()).unsubscribeAll()
                    SyncWorker.setPeriodicEnabled(getApplication(), enabled)
                    if (enabled) SyncWorker.enqueue(getApplication())
                    mutableMessage.value = if (enabled) "Cloud backup enabled. Pending records will sync when connected." else "Cloud backup disabled. Local data remains on this device."
                },
                onFailure = { mutableMessage.value = it.message },
            )
        }
    }
}
