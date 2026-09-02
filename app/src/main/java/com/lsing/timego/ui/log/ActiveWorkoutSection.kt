package com.lsing.timego.ui.log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.categoryVisual
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.NightMint
import com.lsing.timego.ui.theme.Spacing

/**
 * Pinned Active Workout Section that displays the list of exercises and completed sets
 * logged in the current active session.
 */
@Composable
fun ActiveWorkoutSection(
    activeSetsByExercise: Map<Long, List<SetLog>>,
    exercisesById: Map<Long, Exercise>,
    onSelectExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = activeSetsByExercise.isNotEmpty(),
        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
    ) {
        val totalSets = activeSetsByExercise.values.sumOf { it.size }

        SurfaceCard(
            hero = true,
            riveted = true,
            modifier = modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
            ) {
                Text(
                    text = "Current Session",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                AnimatedContent(
                    targetState = totalSets,
                    transitionSpec = {
                        val enter = slideInVertically { height -> height / 2 } + fadeIn()
                        val exit = slideOutVertically { height -> -height / 2 } + fadeOut()
                        enter togetherWith exit
                    },
                    label = "totalSetsCounter",
                ) { count ->
                    Text(
                        text = "$count set${if (count == 1) "" else "s"} logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            activeSetsByExercise.forEach { (exerciseId, sets) ->
                val exercise = exercisesById[exerciseId] ?: return@forEach
                val visual = categoryVisual(exercise.category)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExercise(exerciseId) }
                        .padding(vertical = Spacing.Small),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.ExtraSmall),
                    ) {
                        Icon(
                            visual.icon,
                            contentDescription = null,
                            tint = visual.accent,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp),
                        )
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        AnimatedContent(
                            targetState = sets.size,
                            transitionSpec = {
                                val enter = slideInVertically { height -> height / 2 } + fadeIn()
                                val exit = slideOutVertically { height -> -height / 2 } + fadeOut()
                                enter togetherWith exit
                            },
                            label = "exerciseSetCount",
                        ) { count ->
                            Text(
                                text = "$count set${if (count == 1) "" else "s"}",
                                style = LedgerFigureValue.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Completed sets chips / badges
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        sets.forEachIndexed { index, set ->
                            CompletedSetBadge(index = index + 1, set = set, exercise = exercise)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun CompletedSetBadge(
    index: Int,
    set: SetLog,
    exercise: Exercise,
) {
    val badgeScale = remember { Animatable(0.4f) }
    LaunchedEffect(set.id) {
        badgeScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    val setText = when (exercise.loggingType) {
        LoggingType.HOLD.name -> {
            "${set.holdSeconds ?: 0}s"
        }
        LoggingType.DURATION_DISTANCE.name -> {
            val mins = set.durationMinutes?.let { "%.1fm".format(it) } ?: "--"
            val dist = set.distanceKm?.let { " (${"%.1f".format(it)}km)" } ?: ""
            "$mins$dist"
        }
        else -> {
            val weightStr = if (exercise.category == com.lsing.timego.data.ExerciseCategory.CALISTHENICS.name && set.addedWeightKg != null) {
                formatCalisthenicsWeight(set.addedWeightKg)
            } else {
                "${set.weightKg}kg"
            }
            val rpeStr = set.rpe?.let { " @$it" } ?: ""
            "$weightStr x ${set.reps}$rpeStr"
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer {
                scaleX = badgeScale.value
                scaleY = badgeScale.value
            }
            .clip(CircleShape)
            .background(
                if (set.isWarmup) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (set.isWarmup) MaterialTheme.colorScheme.outline else NightMint),
        ) {
            if (set.isWarmup) {
                Text(
                    "W",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.surface,
                )
            } else {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Text(
            text = " $index: $setText",
            style = LedgerFigureEmphasis.copy(fontSize = 12.sp),
            color = if (set.isWarmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}
