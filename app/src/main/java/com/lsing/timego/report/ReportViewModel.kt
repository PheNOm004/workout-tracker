package com.lsing.timego.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

data class ReportUiState(
    val subscription: ReportSubscription = ReportSubscription(),
    val weeklyPreview: WorkoutReport? = null,
    val monthlyPreview: WorkoutReport? = null,
    val message: String? = null,
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val workoutRepository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private val subscriptionRepository = ReportSubscriptionRepository(application)
    private val preferencesRemote = ReportPreferencesRemote(application)
    private val mutableState = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = mutableState.asStateFlow()

    init {
        // Do not publish the initial local snapshot: on a fresh device it is the default
        // (both cadences disabled) and must not overwrite an existing server preference.
        // Deliberate changes made through this ViewModel emit after the initial snapshot.
        viewModelScope.launch { subscriptionRepository.subscription.drop(1).collect { preferencesRemote.publish(it) } }
        viewModelScope.launch {
            combine(
                workoutRepository.sessions,
                workoutRepository.setLogs,
                workoutRepository.exercises,
                workoutRepository.bodyMetrics,
                subscriptionRepository.subscription,
            ) { sessions, sets, exercises, bodyMetrics, subscription ->
                val input = ReportInput(sessions, sets, exercises, bodyMetrics)
                ReportUiState(
                    subscription = subscription,
                    weeklyPreview = buildWeeklyReport(input, LocalDate.now()),
                    monthlyPreview = buildMonthlyReport(input, YearMonth.now()),
                )
            }.collect { mutableState.value = it }
        }
    }

    fun setCadence(cadence: ReportCadence, enabled: Boolean, emailVerified: Boolean, cloudBackupEnabled: Boolean) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.setCadence(cadence, enabled, emailVerified, cloudBackupEnabled)) {
                SubscriptionResult.Success -> Unit
                is SubscriptionResult.Failure -> mutableState.value = mutableState.value.copy(message = when (result.reason) {
                    SubscriptionFailure.EMAIL_NOT_VERIFIED -> "Verify your email before enabling reports."
                    SubscriptionFailure.CLOUD_BACKUP_REQUIRED -> "Enable cloud backup before enabling reports."
                    SubscriptionFailure.INVALID_TIMEZONE -> "Choose a valid timezone."
                })
            }
        }
    }

    fun unsubscribeAll() { viewModelScope.launch { subscriptionRepository.unsubscribeAll() } }

    fun setTimezone(timezoneId: String) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.setTimezone(timezoneId.trim())) {
                SubscriptionResult.Success -> mutableState.value = mutableState.value.copy(message = "Timezone saved.")
                is SubscriptionResult.Failure -> mutableState.value = mutableState.value.copy(message = "Enter a valid IANA timezone, such as Australia/Sydney.")
            }
        }
    }
}
