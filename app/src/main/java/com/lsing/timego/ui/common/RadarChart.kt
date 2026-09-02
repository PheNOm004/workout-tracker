package com.lsing.timego.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.ui.theme.NightViolet
import com.lsing.timego.ui.theme.TimeGoMotion
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Interpolates each shared axis from [from] toward [to] at [t] (0f..1f); an axis missing from
 *  [from] (e.g. the very first draw) starts from [to]'s own value, so a chart never animates in
 *  from a phantom zero. Key order follows [to], matching the caller's axis order. */
private fun lerpValues(from: Map<String, Float>, to: Map<String, Float>, t: Float): Map<String, Float> =
    to.mapValues { (key, target) ->
        val start = from[key] ?: target
        start + (target - start) * t
    }

private val CANONICAL_RADAR_AXES = listOf(
    "Chest", "Front Delts", "Side Delts", "Rear Delts",
    "Traps", "Upper Back", "Lats", "Lower Back",
    "Biceps", "Triceps", "Forearms",
    "Abs", "Quads", "Hamstrings", "Glutes", "Calves",
)

private fun prepareRadarValues(input: Map<String, Float>): Map<String, Float> {
    val result = linkedMapOf<String, Float>()
    CANONICAL_RADAR_AXES.forEach { axis ->
        result[axis] = input[axis] ?: 0f
    }
    return result
}

/** Spider/radar chart -- pure geometry (16 canonical anatomical axes spaced evenly by angle,
 *  a value polygon, reference rings, and ghost comparison polygon).
 *  Selecting a new timeframe swaps the whole [values]/[comparisonValues] map, smoothly morphing
 *  the polygon over [TimeGoMotion.contentEnter] instead of snapping. */
@Composable
fun RadarChart(
    values: Map<String, Float>,
    modifier: Modifier = Modifier,
    comparisonValues: Map<String, Float> = emptyMap(),
) {
    val resolvedValues = remember(values) { prepareRadarValues(values) }
    val resolvedComparison = remember(comparisonValues) { prepareRadarValues(comparisonValues) }

    val fillColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 11.sp.toPx() }

    val progress = remember { Animatable(1f) }
    var animationStart by remember { mutableStateOf(resolvedValues) }
    var animationTarget by remember { mutableStateOf(resolvedValues) }
    var comparisonAnimationStart by remember { mutableStateOf(resolvedComparison) }
    var comparisonAnimationTarget by remember { mutableStateOf(resolvedComparison) }

    LaunchedEffect(resolvedValues, resolvedComparison) {
        if (resolvedValues != animationTarget || resolvedComparison != comparisonAnimationTarget) {
            animationStart = lerpValues(animationStart, animationTarget, progress.value)
            comparisonAnimationStart = lerpValues(comparisonAnimationStart, comparisonAnimationTarget, progress.value)
            animationTarget = resolvedValues
            comparisonAnimationTarget = resolvedComparison
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = TimeGoMotion.contentEnter)
        }
    }

    val displayedValues = lerpValues(animationStart, animationTarget, progress.value)
    val displayedComparison = lerpValues(comparisonAnimationStart, comparisonAnimationTarget, progress.value)

    Canvas(modifier = modifier) {
        val axisCount = displayedValues.size
        if (axisCount < 3) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = min(size.width, size.height) / 2f - labelTextSizePx * 3.4f

        fun pointFor(index: Int, radiusFraction: Float): Offset {
            val angle = (-Math.PI / 2) + (2 * Math.PI * index / axisCount)
            return Offset(
                center.x + (maxRadius * radiusFraction * cos(angle)).toFloat(),
                center.y + (maxRadius * radiusFraction * sin(angle)).toFloat(),
            )
        }

        fun pointForValue(index: Int, value: Float): Offset =
            pointFor(index, sqrt(value.coerceIn(0f, 1f)))

        // Reference rings represent 1/3, 2/3, and full value under the same display scale.
        listOf(0.33f, 0.66f, 1f).forEach { ringValue ->
            val ringPath = Path()
            for (i in 0 until axisCount) {
                val point = pointForValue(i, ringValue)
                if (i == 0) ringPath.moveTo(point.x, point.y) else ringPath.lineTo(point.x, point.y)
            }
            ringPath.close()
            drawPath(ringPath, color = gridColor.copy(alpha = 0.22f), style = Stroke(width = 1.2f))
        }
        for (i in 0 until axisCount) {
            drawLine(color = gridColor.copy(alpha = 0.22f), start = center, end = pointFor(i, 1f), strokeWidth = 1.2f)
        }

        // Draw comparison ghost polygon if comparison has non-zero values
        val hasComparisonData = displayedComparison.values.any { it > 0.01f }
        if (hasComparisonData) {
            val comparisonPath = Path()
            displayedValues.keys.forEachIndexed { index, label ->
                val point = pointForValue(index, displayedComparison[label] ?: 0f)
                if (index == 0) comparisonPath.moveTo(point.x, point.y) else comparisonPath.lineTo(point.x, point.y)
            }
            comparisonPath.close()
            drawPath(comparisonPath, color = NightViolet.copy(alpha = 0.45f), style = Stroke(width = 2f))
        }

        // The current-period value polygon.
        val hasCurrentData = displayedValues.values.any { it > 0.01f }
        if (hasCurrentData) {
            val valuePath = Path()
            displayedValues.values.forEachIndexed { index, value ->
                val point = pointForValue(index, value)
                if (index == 0) valuePath.moveTo(point.x, point.y) else valuePath.lineTo(point.x, point.y)
            }
            valuePath.close()
            drawPath(valuePath, color = fillColor.copy(alpha = 0.32f))
            drawPath(valuePath, color = fillColor, style = Stroke(width = 2.5f))

            // Draw vertex dots on trained spokes
            displayedValues.values.forEachIndexed { index, value ->
                if (value > 0.05f) {
                    val point = pointForValue(index, value)
                    drawCircle(color = fillColor, radius = 3.5.dp.toPx(), center = point)
                }
            }
        }

        val nativePaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSizePx
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        displayedValues.keys.forEachIndexed { index, label ->
            val labelPoint = pointFor(index, 1.15f)
            nativePaint.textAlign = when {
                labelPoint.x < center.x - labelTextSizePx * 1.5f -> android.graphics.Paint.Align.RIGHT
                labelPoint.x > center.x + labelTextSizePx * 1.5f -> android.graphics.Paint.Align.LEFT
                else -> android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(label, labelPoint.x, labelPoint.y + labelTextSizePx / 3f, nativePaint)
        }
    }
}
