package com.lsing.timego.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsing.timego.report.ReportCadence
import com.lsing.timego.report.ReportViewModel
import com.lsing.timego.report.WorkoutReport
import com.lsing.timego.report.reportSubscriptionBlockReason
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.theme.Spacing

@Composable
fun ReportSettingsScreen(viewModel: ReportViewModel, emailVerified: Boolean, cloudBackupEnabled: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    var timezone by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.subscription.timezoneId) { timezone = state.subscription.timezoneId }
    val blockReason = reportSubscriptionBlockReason(emailVerified, cloudBackupEnabled)
    preview?.let { cadence ->
        val report: WorkoutReport? = if (cadence == "weekly") state.weeklyPreview else state.monthlyPreview
        report?.let { ReportPreviewSheet(it, if (cadence == "weekly") "Weekly report preview" else "Monthly report preview") { preview = null } }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to account") }
            Column { Text("Email reports", style = MaterialTheme.typography.headlineSmall); Text("Both choices are off by default", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        blockReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        ReportCadenceCard("Weekly report", state.subscription.weeklyEnabled, state.weeklyPreview != null, { viewModel.setCadence(ReportCadence.WEEKLY, it, emailVerified, cloudBackupEnabled) }, { preview = "weekly" })
        ReportCadenceCard("Monthly report", state.subscription.monthlyEnabled, state.monthlyPreview != null, { viewModel.setCadence(ReportCadence.MONTHLY, it, emailVerified, cloudBackupEnabled) }, { preview = "monthly" })
        Text("Delivery timezone", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = timezone,
            onValueChange = { timezone = it.take(80) },
            label = { Text("IANA timezone") },
            supportingText = { Text("Example: Australia/Sydney or America/New_York") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { viewModel.setTimezone(timezone) }, modifier = Modifier.fillMaxWidth()) { Text("Save timezone") }
        if (state.subscription.weeklyEnabled || state.subscription.monthlyEnabled) OutlinedButton(onClick = viewModel::unsubscribeAll, modifier = Modifier.fillMaxWidth()) { Text("Unsubscribe from all reports") }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Preview is calculated locally. Email delivery begins only after cloud backup and the report service are deployed and separately enabled.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ReportCadenceCard(title: String, enabled: Boolean, canPreview: Boolean, onEnabled: (Boolean) -> Unit, onPreview: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), hero = true) {
        Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = onEnabled) }
            OutlinedButton(onClick = onPreview, enabled = canPreview, modifier = Modifier.fillMaxWidth()) { Text("Preview") }
        }
    }
}
