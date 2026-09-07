package com.lsing.timego.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.ui.theme.BrassHighlight
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightDeckLow
import com.lsing.timego.ui.theme.NightEdgeHairline
import com.lsing.timego.ui.theme.NightMint
import com.lsing.timego.ui.theme.Spacing

/**
 * High-contrast luminous radar sheen matching the Engine-Room Gauge Panel aesthetic.
 * Sweeps a wide, brilliant metallic brass-steel highlight beam across all skeleton elements.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "timeGoShimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = -400f,
        targetValue = 2200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "timeGoShimmerTranslate",
    )

    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF1B2226),
            Color(0xFF2A343A),
            Color(0xFF42515A),
            BrassHighlight.copy(alpha = 0.55f),
            Color(0xFF42515A),
            Color(0xFF2A343A),
            Color(0xFF1B2226),
        ),
        start = Offset(translateAnimation - 500f, 0f),
        end = Offset(translateAnimation + 200f, 250f),
    )
}

/** Shimmering rounded line/bar representing text or figure placeholders. */
@Composable
fun ShimmerLine(
    width: Dp,
    height: Dp = 12.dp,
    cornerRadius: Dp = 4.dp,
    modifier: Modifier = Modifier,
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
            .border(0.5.dp, NightEdgeHairline, RoundedCornerShape(cornerRadius)),
    )
}

/** Placeholder pill chip for loading filter rows with active border styling. */
@Composable
fun ShimmerChip(
    modifier: Modifier = Modifier,
    width: Dp = 68.dp,
    height: Dp = 32.dp,
    highlighted: Boolean = false,
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
            .border(
                width = if (highlighted) 1.dp else 0.5.dp,
                color = if (highlighted) BrassHighlight.copy(alpha = 0.6f) else NightEdgeHairline,
                shape = RoundedCornerShape(8.dp),
            ),
    )
}

/**
 * Consistency Heatmap Skeleton:
 * Displays a realistic 18-week mock dot grid with month labels, simulating
 * the actual 126-dot canvas layout with a sweeping radar beam.
 */
@Composable
fun ConsistencyHeatmapSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Consistency", topPadding = Spacing.ExtraSmall)
        SurfaceCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
            cornerRadius = 8.dp,
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                // Header month labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("OCT", "NOV", "DEC", "JAN").forEach { month ->
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }

                // 18-week by 7-row mock heatmap dot matrix matching HeatmapGrid exactly
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val availableWidthPx = constraints.maxWidth
                    val density = LocalDensity.current
                    val spacing = 3.dp
                    val spacingPx = with(density) { spacing.roundToPx() }
                    val dotPx = (availableWidthPx - spacingPx * 17) / 18
                    val dotSize = with(density) { dotPx.toDp() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                    ) {
                        repeat(18) { col ->
                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                repeat(7) { row ->
                                    val cellAlpha = 0.35f + ((row * 3 + col * 5) % 5) * 0.13f
                                    Box(
                                        modifier = Modifier
                                            .size(dotSize)
                                            .clip(CircleShape)
                                            .background(brush)
                                            .border(0.75.dp, NightEdgeHairline.copy(alpha = cellAlpha), CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Less",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(brush)
                                .border(0.75.dp, NightEdgeHairline, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        "More",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

/**
 * Muscle Distribution Skeleton:
 * Shows filter chips, an authentic anatomical silhouette wireframe blueprint,
 * and 4 symmetric gauge stat tiles in a clean 2x2 grid.
 */
@Composable
fun MuscleDistributionSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val frontShapes = remember { buildShapes(FRONT_BODY_PATHS, FRONT_BODY_VIEWBOX) }
    val backShapes = remember { buildShapes(BACK_BODY_PATHS, BACK_BODY_VIEWBOX) }
    val frontAspect = (FRONT_BODY_VIEWBOX[2] - FRONT_BODY_VIEWBOX[0]) / (FRONT_BODY_VIEWBOX[3] - FRONT_BODY_VIEWBOX[1])
    val backAspect = (BACK_BODY_VIEWBOX[2] - BACK_BODY_VIEWBOX[0]) / (BACK_BODY_VIEWBOX[3] - BACK_BODY_VIEWBOX[1])

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Muscle Distribution", topPadding = Spacing.Small)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            ShimmerChip(width = 64.dp, highlighted = true)
            ShimmerChip(width = 72.dp)
            ShimmerChip(width = 68.dp)
            ShimmerChip(width = 56.dp)
        }

        // Anatomical wireframe blueprint card
        SurfaceCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
            hero = true,
            cornerRadius = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(Spacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Two wireframe silhouettes (Front and Back) matching the real muscle diagram
                Row(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "ANTERIOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .aspectRatio(frontAspect),
                        ) {
                            val scaleFactor = size.height / (FRONT_BODY_VIEWBOX[3] - FRONT_BODY_VIEWBOX[1])
                            scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                                frontShapes.forEach { shape ->
                                    drawPath(
                                        path = shape.path,
                                        brush = brush,
                                    )
                                    drawPath(
                                        path = shape.path,
                                        color = NightEdgeHairline.copy(alpha = 0.6f),
                                        style = Stroke(width = 1f),
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "POSTERIOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .aspectRatio(backAspect),
                        ) {
                            val scaleFactor = size.height / (BACK_BODY_VIEWBOX[3] - BACK_BODY_VIEWBOX[1])
                            scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                                backShapes.forEach { shape ->
                                    drawPath(
                                        path = shape.path,
                                        brush = brush,
                                    )
                                    drawPath(
                                        path = shape.path,
                                        color = NightEdgeHairline.copy(alpha = 0.6f),
                                        style = Stroke(width = 1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 StatTile skeleton cards in a perfectly balanced 2x2 grid
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.ExtraSmall),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                StatTileSkeleton(label = "Workouts", modifier = Modifier.weight(1f), brush = brush)
                StatTileSkeleton(label = "Duration", modifier = Modifier.weight(1f), brush = brush)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                StatTileSkeleton(label = "Volume", modifier = Modifier.weight(1f), brush = brush)
                StatTileSkeleton(label = "Sets", modifier = Modifier.weight(1f), brush = brush)
            }
        }
    }
}

@Composable
private fun StatTileSkeleton(
    label: String,
    modifier: Modifier = Modifier,
    brush: Brush,
) {
    SurfaceCard(
        modifier = modifier,
        cornerRadius = 4.dp,
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
                    .border(0.5.dp, NightEdgeHairline, RoundedCornerShape(4.dp)),
            )
        }
    }
}

/**
 * Exercise Performance Skeleton:
 * Shows curve mode chips, exercise selector wheel, and a progression
 * chart card with mock sinusoidal wave and datum gridlines.
 */
@Composable
fun ExercisePerformanceSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Exercise Performance", topPadding = Spacing.Small)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            ShimmerChip(width = 96.dp, highlighted = true)
            ShimmerChip(width = 120.dp)
        }

        // Exercise wheel selector mockup
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            ShimmerChip(width = 160.dp, height = 34.dp, highlighted = true)
        }

        // Chart with glowing progression curve
        SurfaceCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
            hero = true,
            cornerRadius = 8.dp,
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                ShimmerLine(width = 130.dp, height = 12.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Canvas showing datum grid lines and glowing wave
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = 8.dp),
                ) {
                    val width = size.width
                    val height = size.height
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // 3 horizontal gridlines
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = NightEdgeHairline,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            pathEffect = dashEffect,
                            strokeWidth = 1f,
                        )
                    }

                    // Stylized progression curve
                    val path = Path().apply {
                        moveTo(0f, height * 0.75f)
                        cubicTo(
                            width * 0.3f, height * 0.7f,
                            width * 0.45f, height * 0.35f,
                            width * 0.7f, height * 0.4f,
                        )
                        cubicTo(
                            width * 0.82f, height * 0.42f,
                            width * 0.9f, height * 0.2f,
                            width, height * 0.15f,
                        )
                    }
                    drawPath(
                        path = path,
                        brush = brush,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    // Peak target dot
                    drawCircle(
                        color = NightMint,
                        radius = 4.dp.toPx(),
                        center = Offset(width, height * 0.15f),
                    )
                }

                // Bottom date baseline indicators
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("Week 1", "Week 6", "Week 12", "Current").forEach { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Routine Card Skeleton:
 * Shows a structured routine card with title, exercise count, and 7 circular day badges.
 */
@Composable
fun RoutineCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    SurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        cornerRadius = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(width = 140.dp, height = 15.dp, cornerRadius = 4.dp)
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerLine(width = 85.dp, height = 11.dp, cornerRadius = 3.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // 7 circular day pills (Mon - Sun)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { index, day ->
                        val active = (index == 0 || index == 2 || index == 4)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(brush)
                                .border(
                                    width = if (active) 1.dp else 0.5.dp,
                                    color = if (active) BrassHighlight.copy(alpha = 0.7f) else NightEdgeHairline,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (active) BrassHighlight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // Right action arrow
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(brush)
                    .border(0.5.dp, NightEdgeHairline, CircleShape),
            )
        }
    }
}
