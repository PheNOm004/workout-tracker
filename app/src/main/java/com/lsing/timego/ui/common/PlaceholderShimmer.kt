package com.lsing.timego.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightDeckLow
import com.lsing.timego.ui.theme.NightEdgeHairline
import com.lsing.timego.ui.theme.Spacing

/**
 * Sweeping metallic highlight brush matching the Engine-Room Gauge Panel aesthetic.
 * Runs continuously on an infinite transition to provide a sleek, high-tech radar/sheen
 * across dark steel plates during cold initialization and data hydration.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "timeGoShimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "timeGoShimmerTranslate",
    )

    return Brush.linearGradient(
        colors = listOf(
            NightDeckLow,
            NightDeckHigh,
            Color(0xFF282F3A),
            NightDeckHigh,
            NightDeckLow,
        ),
        start = Offset(translateAnimation - 350f, translateAnimation - 350f),
        end = Offset(translateAnimation, translateAnimation),
    )
}

/** A single raised surface card displaying the animated gauge sheen. */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp,
    cornerRadius: Dp = 8.dp,
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
            .border(1.dp, NightEdgeHairline, RoundedCornerShape(cornerRadius)),
    )
}

/** Placeholder pill chip for loading filter rows. */
@Composable
fun ShimmerChip(
    modifier: Modifier = Modifier,
    width: Dp = 68.dp,
    height: Dp = 32.dp,
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
            .border(1.dp, NightEdgeHairline, RoundedCornerShape(8.dp)),
    )
}

/** Skeleton placeholder for the 18-week Consistency Heatmap card. */
@Composable
fun ConsistencyHeatmapSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Consistency", topPadding = Spacing.ExtraSmall)
        ShimmerCard(height = 126.dp)
    }
}

/** Skeleton placeholder for the Muscle Distribution diagram and stat tiles. */
@Composable
fun MuscleDistributionSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Muscle Distribution", topPadding = Spacing.Small)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            ShimmerChip(width = 64.dp)
            ShimmerChip(width = 72.dp)
            ShimmerChip(width = 68.dp)
            ShimmerChip(width = 56.dp)
        }
        ShimmerCard(
            height = 210.dp,
            modifier = Modifier.padding(vertical = Spacing.ExtraSmall),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(vertical = 4.dp),
                ) {
                    ShimmerCard(height = 68.dp)
                }
            }
        }
    }
}

/** Skeleton placeholder for the Exercise Performance progression curve and PR cards. */
@Composable
fun ExercisePerformanceSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Exercise Performance", topPadding = Spacing.Small)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            ShimmerChip(width = 96.dp)
            ShimmerChip(width = 120.dp)
        }
        ShimmerCard(
            height = 180.dp,
            modifier = Modifier.padding(vertical = Spacing.ExtraSmall),
        )
    }
}

/** Skeleton placeholder for a Routine card on the Routines screen. */
@Composable
fun RoutineCardSkeleton(modifier: Modifier = Modifier) {
    ShimmerCard(
        height = 88.dp,
        modifier = modifier.padding(vertical = Spacing.Small),
        cornerRadius = 10.dp,
    )
}
