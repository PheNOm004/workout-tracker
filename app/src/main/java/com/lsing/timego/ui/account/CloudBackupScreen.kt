package com.lsing.timego.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsing.timego.sync.CloudBackupViewModel
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.theme.Spacing

@Composable
fun CloudBackupScreen(viewModel: CloudBackupViewModel, emailVerified: Boolean, onBack: () -> Unit) {
    val consent by viewModel.consent.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to account") }
            Column { Text("Cloud backup", style = MaterialTheme.typography.headlineSmall); Text("Optional and off by default", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        SurfaceCard(modifier = Modifier.fillMaxWidth(), hero = true) {
            Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text("First upload preview", style = MaterialTheme.typography.titleMedium)
                Text("$pendingCount local changes are waiting. Existing history will not be replaced silently.")
                Text("Your private free-text limitation note is excluded. Account creation alone never enables backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!emailVerified) Text("Verify your email before enabling backup.", color = MaterialTheme.colorScheme.error)
        if (!viewModel.backendAvailable) Text("Firebase is not configured in this build.", color = MaterialTheme.colorScheme.error)
        if (consent.enabled) OutlinedButton(onClick = { viewModel.setEnabled(false, emailVerified) }, modifier = Modifier.fillMaxWidth()) { Text("Disable cloud backup") }
        else Button(onClick = { viewModel.setEnabled(true, emailVerified) }, enabled = emailVerified && viewModel.backendAvailable, modifier = Modifier.fillMaxWidth()) { Text("Enable cloud backup") }
        message?.let { Text(it) }
    }
}
