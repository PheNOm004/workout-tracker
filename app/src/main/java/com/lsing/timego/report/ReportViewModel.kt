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
    private val mutableState = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                workoutRepository.sessions,
                workoutRepository.setLogs,
                workoutRepository.exercises,
                subscriptionRepository.subscription,
            ) { sessions, sets, exercises, subscription ->
                val input = ReportInput(sessions, sets, exercises)
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
}
