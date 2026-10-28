package com.lsing.timego.ui.onboarding

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    androidx.activity.compose.BackHandler(enabled = state.step != OnboardingStep.INTRO) { viewModel.back() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Large),
        ) {
            Text(
                "TIMEGO PROFILE  ·  STEP ${state.step.ordinal + 1} OF ${OnboardingStep.entries.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = (state.step.ordinal + 1f) / OnboardingStep.entries.size,
                label = "onboardingProgress",
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small, bottom = Spacing.Medium),
            )
            // Same horizontal slide the rest of the app uses: forward slides left, back slides right.
            androidx.compose.animation.AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val enter = androidx.compose.animation.slideInHorizontally(com.lsing.timego.ui.theme.TimeGoMotion.navigationInOffset) { w -> if (forward) w / 5 else -w / 5 } +
                        androidx.compose.animation.fadeIn(com.lsing.timego.ui.theme.TimeGoMotion.contentEnter)
                    val exit = androidx.compose.animation.slideOutHorizontally(com.lsing.timego.ui.theme.TimeGoMotion.navigationOutOffset) { w -> if (forward) -w / 5 else w / 5 } +
                        androidx.compose.animation.fadeOut(com.lsing.timego.ui.theme.TimeGoMotion.contentExit)
                    enter togetherWith exit
                },
                label = "onboardingStep",
            ) { step ->
                Column {
                    Text(stepTitle(step), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stepExplanation(step),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Medium),
                    )
                    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Large)) {
                            StepContent(state = state.copy(step = step), update = viewModel::updateDraft, onEditStep = viewModel::goTo)
                        }
                    }
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
    onEditStep: (OnboardingStep) -> Unit,
) {
    val draft = state.draft
    when (state.step) {
        OnboardingStep.INTRO -> Text(
            "Your answers set transparent starting preferences. Workout history becomes stronger evidence over time. " +
                "TimeGo still works offline and every answer can be changed later.",
        )
        OnboardingStep.GOAL -> OptionList(TrainingGoal.entries, draft.goal) { update { p -> p.copy(goal = it) } }
        OnboardingStep.EXPERIENCE -> OptionList(ExperienceLevel.entries, draft.experience) {
            update { p -> p.copy(experience = it) }
        }
        OnboardingStep.MODALITY -> OptionList(TrainingLean.entries, draft.modality) {
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
            var imperialUnits by rememberSaveable { mutableStateOf(false) }
            OutlinedTextField(
                value = draft.displayName.orEmpty(),
                onValueChange = { value -> update { it.copy(displayName = value.take(60)) } },
                label = { Text("Name or nickname (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Age range (optional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.Medium))
            ChoiceFlow(AgeRange.entries, draft.ageRange) { update { p -> p.copy(ageRange = it) } }
            Text("Units", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.Medium))
            ChoiceFlow(listOf("Metric", "Imperial"), if (imperialUnits) "Imperial" else "Metric") {
                imperialUnits = it == "Imperial"
            }
            NumericProfileField(
                label = "Height in ${if (imperialUnits) "in" else "cm"} (optional)",
                value = draft.heightCm?.let { if (imperialUnits) it / 2.54 else it },
            ) { value -> update { it.copy(heightCm = value?.let { raw -> if (imperialUnits) raw * 2.54 else raw }) } }
            NumericProfileField(
                label = "Weight in ${if (imperialUnits) "lb" else "kg"} (optional)",
                value = draft.weightKg?.let { if (imperialUnits) it / 0.45359237 else it },
            ) { value -> update { it.copy(weightKg = value?.let { raw -> if (imperialUnits) raw * 0.45359237 else raw }) } }
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
            ReviewRow("Goal", draft.goal) { onEditStep(OnboardingStep.GOAL) }
            ReviewRow("Experience", draft.experience) { onEditStep(OnboardingStep.EXPERIENCE) }
            ReviewRow("Preference", draft.modality) { onEditStep(OnboardingStep.MODALITY) }
            ReviewRow("Equipment", draft.equipment.joinToString { formatEnumLabel(it.name) }) { onEditStep(OnboardingStep.EQUIPMENT) }
            ReviewRow("Training days", draft.trainingDaysPerWeek) { onEditStep(OnboardingStep.SCHEDULE) }
            ReviewRow("Session", draft.sessionDuration) { onEditStep(OnboardingStep.SCHEDULE) }
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
private fun ReviewRow(label: String, value: Any?, onChange: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onChange).heightIn(min = 48.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value?.let(::optionLabel) ?: "Not provided", style = MaterialTheme.typography.bodyLarge)
        }
        Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** Single-choice question as full-width rows with a one-line explanation, easier to read and tap than chips. */
@Composable
private fun <T : Any> OptionList(options: List<T>, selected: T?, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(Spacing.Medium),
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Column(modifier = Modifier.padding(start = Spacing.Medium)) {
                    Text(optionLabel(option), style = MaterialTheme.typography.titleSmall)
                    optionDescription(option)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun optionDescription(value: Any): String? = when (value) {
    TrainingGoal.GENERAL_FITNESS -> "Stay active, balanced and healthy."
    TrainingGoal.STRENGTH -> "Lift heavier over time."
    TrainingGoal.HYPERTROPHY -> "Build muscle size."
    TrainingGoal.ENDURANCE -> "More reps, work capacity and conditioning."
    TrainingGoal.SKILL -> "Calisthenics skills such as handstands and levers."
    ExperienceLevel.BEGINNER -> "New to structured training."
    ExperienceLevel.NOVICE -> "Roughly 6 to 18 months of consistent training."
    ExperienceLevel.INTERMEDIATE -> "Around 1.5 to 4 years; progress is slower."
    ExperienceLevel.ADVANCED -> "4+ years; progress is very gradual."
    TrainingLean.STRENGTH -> "Barbells, dumbbells and machines."
    TrainingLean.BALANCED -> "A mix of weights and bodyweight."
    TrainingLean.CALISTHENICS -> "Bodyweight movements and progressions."
    else -> null
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
