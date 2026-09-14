package com.lsing.timego.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsing.timego.account.AccountViewModel
import com.lsing.timego.account.AuthState
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(viewModel: AccountViewModel, onOpenReports: () -> Unit, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings") }
            Column(modifier = Modifier.padding(start = Spacing.Small)) {
                Text("Account", style = MaterialTheme.typography.headlineSmall)
                Text("Optional — TimeGo remains usable without signing in", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (val auth = state.authState) {
            AuthState.Guest -> {
                SurfaceCard(modifier = Modifier.fillMaxWidth(), hero = true) {
                    Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        SectionHeader(if (registering) "Create account" else "Sign in")
                        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                        if (registering) OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        Button(
                            enabled = !state.busy,
                            onClick = { scope.launch { if (registering) viewModel.register(email, password, confirmation) else viewModel.signIn(email, password) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (registering) "Create account" else "Sign in") }
                        TextButton(onClick = { registering = !registering }) { Text(if (registering) "I already have an account" else "Create an account") }
                        TextButton(onClick = { scope.launch { viewModel.resetPassword(email) } }) { Text("Forgot password?") }
                    }
                }
            }
            is AuthState.SignedIn -> {
                Text(auth.email, style = MaterialTheme.typography.titleMedium)
                Text(if (auth.verified) "Email verified" else "Email verification required", color = if (auth.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                if (!auth.verified) Button(onClick = { scope.launch { viewModel.sendVerification() } }) { Text("Send verification email") }
                OutlinedButton(onClick = { scope.launch { viewModel.signOut() } }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                Text("Cloud backup and email reports require a verified email and separate opt-in. They are never enabled by account creation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Account deletion will be enabled only with the complete cloud-data deletion service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.message?.let { Text(it, color = if (it.contains("requested") || it.contains("deleted")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onOpenReports, modifier = Modifier.fillMaxWidth()) { Text("Weekly and monthly reports") }
    }
}
