package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.domain.primaryMuscleGroups
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightEyebrow
import com.lsing.timego.ui.theme.Spacing
import com.lsing.timego.ui.theme.TimeGoMotion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** [setDescriptions] holds one entry per set of this exercise (e.g. "60.0kg x 8"), grouped
 *  together under a single [exerciseName] row rather than repeating the name once per set --
 *  see [buildDayHistoryEntries]. */
data class DayHistoryEntry(val exerciseName: String, val setDescriptions: List<String>)

/** Group of logged exercises belonging to the same body region, sorted by training volume. */
data class WorkoutHistoryGroup(
    val regionLabel: String,
    val totalSets: Int,
    val entries: List<DayHistoryEntry>,
)

internal fun formatHistoryDuration(durationMinutes: Double?): String {
    val totalSeconds = ((durationMinutes ?: 0.0).coerceAtLeast(0.0) * 60).roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0 -> "${seconds}s"
        seconds == 0 -> "$minutes min"
        else -> "${minutes}m ${seconds}s"
    }
}

/** Shared by the Progress screen's tap-a-heatmap-day history and the logging landing page's
 *  last-session detail -- both need "every set logged in this session/day, one row per exercise
 *  with all its sets listed together" rather than one row per raw set (which repeated the
 *  exercise name for every set of a multi-set exercise). Order follows first-appearance order in
 *  [setLogs] (chronological, since callers pass sets already ordered by loggedAtEpochMillis). */
fun buildDayHistoryEntries(setLogs: List<SetLog>, exercisesById: Map<Long, Exercise>): List<DayHistoryEntry> =
    setLogs
        .mapNotNull { log ->
            val exercise = exercisesById[log.exerciseId] ?: return@mapNotNull null
            val description = when (exercise.loggingType) {
                LoggingType.DURATION_DISTANCE.name -> {
                    val distance = log.distanceKm?.let { " -- ${it}km" } ?: ""
                    "${formatHistoryDuration(log.durationMinutes)}$distance"
                }
                LoggingType.HOLD.name -> "${log.holdSeconds ?: 0}s hold"
                else -> if (exercise.category == ExerciseCategory.CALISTHENICS.name && log.addedWeightKg != null) {
                    "${formatCalisthenicsWeight(log.addedWeightKg)} x ${log.reps}"
                } else {
                    "${log.weightKg}kg x ${log.reps}"
                }
            }
            exercise.name to description
        }
        .groupBy({ it.first }, { it.second })
        .map { (name, descriptions) -> DayHistoryEntry(name, descriptions) }

/**
 * Builds evidence-based grouped workout history entries organized by anatomical display region
 * (Legs, Shoulders, Back, Chest, Arms, Core, Cardio). Regions are ordered descending by total set volume.
 */
fun buildGroupedDayHistory(
    setLogs: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
): List<WorkoutHistoryGroup> {
    if (setLogs.isEmpty()) return emptyList()

    val hasBackMuscles = setLogs.any { log ->
        val ex = exercisesById[log.exerciseId] ?: return@any false
        val pGroups = primaryMuscleGroups(ex).ifEmpty { ex.muscleGroups.toSet() }
        pGroups.any { it in setOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.LOWER_BACK.name, MuscleGroup.TRAPS.name) }
    }

    val dayEntries = buildDayHistoryEntries(setLogs, exercisesById)
    val exercisesByName = exercisesById.values.associateBy { it.name }
    val entriesByRegion = linkedMapOf<String, MutableList<DayHistoryEntry>>()

    for (entry in dayEntries) {
        val exercise = exercisesByName[entry.exerciseName]
        val regionLabel = if (exercise == null) {
            "Other"
        } else {
            exerciseDisplayRegion(exercise, hasBackMuscles)
        }
        entriesByRegion.getOrPut(regionLabel) { mutableListOf() }.add(entry)
    }

    return entriesByRegion.map { (region, entries) ->
        val totalSets = entries.sumOf { it.setDescriptions.size }
        WorkoutHistoryGroup(
            regionLabel = region,
            totalSets = totalSets,
            entries = entries,
        )
    }.sortedByDescending { it.totalSets }
}

/** Shared by the Progress screen's tap-a-heatmap-day history and the logging landing page's
 *  tap-the-summary-card history. When [label] is non-null it appears as a subtitle below the date
 *  (e.g. the routine name that was run that day). Duration appears on its own line when present.
 *  Uses the signature [TimeGoDialog] hardware plate with top brass bezel sheen and NightDeckHigh plate. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutHistoryDialog(
    title: String,
    entries: List<DayHistoryEntry>,
    onDismiss: () -> Unit,
    label: String? = null,
    durationMinutes: Double? = null,
    groupedEntries: List<WorkoutHistoryGroup> = emptyList(),
    date: LocalDate? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightDeckHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Eyebrow
            Text(
                text = "WORKOUT SUMMARY",
                style = NightEyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp),
            )

            // Formatted date (HeatP style: EEEE, MMM d, yyyy)
            val formattedDateText = remember(title, date) {
                date?.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    ?: runCatching {
                        val rawDate = if (title.startsWith("Workout on ")) title.removePrefix("Workout on ").trim() else title.trim()
                        LocalDate.parse(rawDate).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    }.getOrDefault(title)
            }

            Text(
                text = formattedDateText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            // Subtitle & Pills row (Routine name + Duration pill)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (durationMinutes != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatHistoryDuration(durationMinutes),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Body: Empty state or Groups
            if (entries.isEmpty() && groupedEntries.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.EventAvailable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No exercises logged",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No sets recorded for this date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else if (groupedEntries.isNotEmpty()) {
                groupedEntries.forEachIndexed { groupIndex, group ->
                    if (groupIndex > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    // Section Header (HeatP style)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = group.regionLabel.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "${group.totalSets} set${if (group.totalSets == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    // Exercise items card (HeatP style clean card)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            group.entries.forEachIndexed { index, entry ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = entry.exerciseName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 6.dp),
                                    ) {
                                        entry.setDescriptions.forEach { setDesc ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            ) {
                                                Text(
                                                    text = setDesc,
                                                    style = LedgerFigureValue.copy(fontSize = 12.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = entry.exerciseName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 6.dp),
                                ) {
                                    entry.setDescriptions.forEach { setDesc ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            Text(
                                                text = setDesc,
                                                style = LedgerFigureValue.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null) {
    SurfaceCard(
        modifier = modifier.padding(Spacing.ExtraSmall),
        cornerRadius = 4.dp,
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val enter = slideInVertically(TimeGoMotion.navigationInOffset) { height -> height / 2 } + fadeIn(TimeGoMotion.fadeEnter)
                    val exit = slideOutVertically(TimeGoMotion.navigationOutOffset) { height -> -height / 2 } + fadeOut(TimeGoMotion.fadeExit)
                    enter togetherWith exit
                },
                label = "statTileValueTransition",
            ) { targetValue ->
                Text(targetValue, style = LedgerFigureValue, color = MaterialTheme.colorScheme.onSurface)
            }
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
