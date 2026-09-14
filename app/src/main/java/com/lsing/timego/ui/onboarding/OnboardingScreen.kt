package com.lsing.timego.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.profile.AgeRange
import com.lsing.timego.profile.Equipment
import com.lsing.timego.profile.ExperienceLevel
import com.lsing.timego.profile.MovementLimitation
import com.lsing.timego.profile.SessionDurationRange
import com.lsing.timego.profile.TrainingGoal
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.Spacing

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) {
        if (state.completed) onFinished()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Large),
        ) {
            Text("TIMEGO PROFILE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(stepTitle(state.step), style = MaterialTheme.typography.headlineMedium)
            Text(
                stepExplanation(state.step),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Medium),
            )
            LinearProgressIndicator(
                progress = { (state.step.ordinal + 1f) / OnboardingStep.entries.size },
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Large),
            )

            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Large)) {
                    StepContent(state = state, update = viewModel::updateDraft)
                }
            }

            if (state.step == OnboardingStep.REVIEW) {
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium),
                ) { Text("Reset answers") }
            }

            state.validationMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = Spacing.Small))
            }
            Spacer(Modifier.height(Spacing.Large))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (state.step == OnboardingStep.INTRO) {
                    OutlinedButton(onClick = viewModel::dismiss, modifier = Modifier.weight(1f)) { Text("Not now") }
                } else {
                    OutlinedButton(onClick = viewModel::back, modifier = Modifier.weight(1f)) { Text("Back") }
                }
                when {
                    state.step == OnboardingStep.REVIEW -> Button(
                        onClick = viewModel::complete,
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.saving) "Saving…" else "Save profile") }
                    state.step == OnboardingStep.PHYSICAL || state.step == OnboardingStep.LIMITATIONS -> {
                        OutlinedButton(onClick = viewModel::skipOptional, modifier = Modifier.weight(1f)) { Text("Skip") }
                        Button(onClick = viewModel::next, modifier = Modifier.weight(1f)) { Text("Continue") }
                    }
                    else -> Button(onClick = viewModel::next, modifier = Modifier.weight(1f)) { Text("Continue") }
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    state: OnboardingState,
    update: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    val draft = state.draft
    when (state.step) {
        OnboardingStep.INTRO -> Text(
            "Your answers set transparent starting preferences. Workout history becomes stronger evidence over time. " +
                "TimeGo still works offline and every answer can be changed later.",
        )
        OnboardingStep.GOAL -> ChoiceFlow(TrainingGoal.entries, draft.goal) { update { p -> p.copy(goal = it) } }
        OnboardingStep.EXPERIENCE -> ChoiceFlow(ExperienceLevel.entries, draft.experience) {
            update { p -> p.copy(experience = it) }
        }
        OnboardingStep.MODALITY -> ChoiceFlow(TrainingLean.entries, draft.modality) {
            update { p -> p.copy(modality = it) }
        }
        OnboardingStep.EQUIPMENT -> ChoiceFlow(Equipment.entries, null, draft.equipment) { equipment ->
            update { p ->
                p.copy(equipment = if (equipment in p.equipment) p.equipment - equipment else p.equipment + equipment)
            }
        }
        OnboardingStep.SCHEDULE -> {
            Text("Training days per week", style = MaterialTheme.typography.titleMedium)
            ChoiceFlow((1..7).toList(), draft.trainingDaysPerWeek) { update { p -> p.copy(trainingDaysPerWeek = it) } }
            Text("Typical session duration", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.Medium))
            ChoiceFlow(SessionDurationRange.entries, draft.sessionDuration) {
                update { p -> p.copy(sessionDuration = it) }
            }
        }
        OnboardingStep.PHYSICAL -> {
            OutlinedTextField(
                value = draft.displayName.orEmpty(),
                onValueChange = { value -> update { it.copy(displayName = value.take(60)) } },
                label = { Text("Name or nickname (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Age range (optional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.Medium))
            ChoiceFlow(AgeRange.entries, draft.ageRange) { update { p -> p.copy(ageRange = it) } }
            NumericProfileField("Height in cm (optional)", draft.heightCm) { value -> update { it.copy(heightCm = value) } }
            NumericProfileField("Weight in kg (optional)", draft.weightKg) { value -> update { it.copy(weightKg = value) } }
        }
        OnboardingStep.LIMITATIONS -> {
            Text("Avoid suggesting movements that rely heavily on these areas. This is not medical advice.")
            ChoiceFlow(MovementLimitation.entries, null, draft.movementLimitations) { limitation ->
                update { p ->
                    p.copy(
                        movementLimitations = if (limitation in p.movementLimitations) {
                            p.movementLimitations - limitation
                        } else {
                            p.movementLimitations + limitation
                        },
                    )
                }
            }
            OutlinedTextField(
                value = draft.privateLimitationNote.orEmpty(),
                onValueChange = { value -> update { it.copy(privateLimitationNote = value.take(300)) } },
                label = { Text("Private note (optional, never synced)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium),
            )
        }
        OnboardingStep.REVIEW -> {
            ReviewRow("Goal", draft.goal)
            ReviewRow("Experience", draft.experience)
            ReviewRow("Preference", draft.modality)
            ReviewRow("Equipment", draft.equipment.joinToString { formatEnumLabel(it.name) })
            ReviewRow("Training days", draft.trainingDaysPerWeek)
            ReviewRow("Session", draft.sessionDuration)
            Text(
                "Used for recommendations: goal, experience, preference, equipment, schedule and structured exclusions. " +
                    "Height, weight, age range and private notes never set a training load automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.Medium),
            )
        }
    }
}

@Composable
private fun <T : Any> ChoiceFlow(
    options: List<T>,
    selected: T?,
    selectedSet: Set<T> = emptySet(),
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected || option in selectedSet,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) },
            )
        }
    }
}

@Composable
private fun NumericProfileField(label: String, value: Double?, onValue: (Double?) -> Unit) {
    var text by rememberSaveable(label) { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { updated ->
            text = updated.take(8)
            onValue(text.replace(',', '.').toDoubleOrNull())
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium),
    )
}

@Composable
private fun ReviewRow(label: String, value: Any?) {
    Text("$label: ${value?.let(::optionLabel) ?: "Not provided"}", modifier = Modifier.padding(vertical = 4.dp))
}

private fun optionLabel(value: Any): String = when (value) {
    is Enum<*> -> formatEnumLabel(value.name.removePrefix("AGE_"))
    else -> value.toString()
}

private fun stepTitle(step: OnboardingStep): String = when (step) {
    OnboardingStep.INTRO -> "Train with better context"
    OnboardingStep.GOAL -> "What are you training for?"
    OnboardingStep.EXPERIENCE -> "What is your experience?"
    OnboardingStep.MODALITY -> "How do you like to train?"
    OnboardingStep.EQUIPMENT -> "What equipment can you use?"
    OnboardingStep.SCHEDULE -> "What fits your week?"
    OnboardingStep.PHYSICAL -> "Physical details"
    OnboardingStep.LIMITATIONS -> "Movement preferences"
    OnboardingStep.REVIEW -> "Review your profile"
}

private fun stepExplanation(step: OnboardingStep): String = when (step) {
    OnboardingStep.INTRO -> "A short setup helps TimeGo choose relevant exercises and explain why."
    OnboardingStep.GOAL -> "This shapes candidate ranking, not guaranteed outcomes."
    OnboardingStep.EXPERIENCE -> "TimeGo uses this as a conservative starting point."
    OnboardingStep.MODALITY -> "Your workout history can outweigh this preference later."
    OnboardingStep.EQUIPMENT -> "Unavailable equipment will be excluded from suggestions."
    OnboardingStep.SCHEDULE -> "Used to keep recommendations realistic for your time."
    OnboardingStep.PHYSICAL -> "Optional and editable. These values do not prescribe loads."
    OnboardingStep.LIMITATIONS -> "Optional exclusions and a phone-only private note."
    OnboardingStep.REVIEW -> "Nothing is uploaded by completing this profile."
}
