package com.lsing.timego.ui.common

import com.lsing.timego.ui.theme.PlateYellow
import com.lsing.timego.ui.theme.PlateSteel
import com.lsing.timego.ui.theme.PlateGreen
import com.lsing.timego.ui.theme.LedgerFigureHero
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.ui.theme.PlateBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

private val AxisDate = DateTimeFormatter.ofPattern("d MMM")

/**
 * Plain Canvas line chart (no charting dependency). Smooth curve with a soft gradient fill, three light
 * gridlines with value labels on the left, a dashed average line, first and last dates underneath, and the
 * latest point called out with a halo and value tag. The curve draws in left to right when the data changes.
 * Shared by the Progress screen's strength curves and the body-weight curve.
 */
@Composable
fun SparklineChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val lineColor = PlateBlue
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val tagColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val tagTextColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 11.sp.toPx() }
    val leftPx = with(density) { 36.dp.toPx() }
    val rightPx = with(density) { 14.dp.toPx() }
    val topPx = with(density) { 16.dp.toPx() }
    val bottomPx = with(density) { 26.dp.toPx() }

    // Reveal progress 0..1; restarts whenever the data changes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(points) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessVeryLow))
    }

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val paint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSizePx
            isAntiAlias = true
        }
        if (points.size == 1) {
            // One point (every entry so far falls on a single day) can't draw a line; show the point and its value.
            val (date, value) = points.first()
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(lineColor.copy(alpha = 0.22f), radius = 14.dp.toPx(), center = center)
            drawCircle(lineColor, radius = 5.dp.toPx(), center = center)
            paint.textAlign = android.graphics.Paint.Align.CENTER
            drawContext.canvas.nativeCanvas.drawText("$date: %.1f".format(value), center.x, center.y - 24.dp.toPx(), paint)
            return@Canvas
        }

        val maxValue = points.maxOf { it.second }
        val minValue = points.minOf { it.second }
        val average = points.map { it.second }.average()
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val plotWidth = size.width - leftPx - rightPx
        val plotHeight = size.height - topPx - bottomPx
        val plotBottom = topPx + plotHeight
        val stepX = plotWidth / (points.size - 1)
        fun xFor(index: Int) = leftPx + stepX * index
        fun yFor(value: Double) = topPx + plotHeight - ((value - minValue) / range * plotHeight).toFloat()
        val fmt = if (range >= 10.0) "%.0f" else "%.1f"

        // Gridlines and axis labels at min, mid and max.
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        listOf(minValue, minValue + range / 2.0, minValue + range).forEach { v ->
            val y = yFor(v)
            drawLine(gridColor, Offset(leftPx, y), Offset(size.width - rightPx, y), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(fmt.format(v), leftPx - 8.dp.toPx(), y + labelTextSizePx * 0.35f, paint)
        }

        // Smooth curve: Catmull-Rom converted to cubic segments, with control points clamped between each
        // pair of neighbours so the line never overshoots a data point.
        val curve = Path()
        points.forEachIndexed { i, (_, v) ->
            val x = xFor(i)
            val y = yFor(v)
            if (i == 0) {
                curve.moveTo(x, y)
            } else {
                val prevX = xFor(i - 1)
                val prevY = yFor(points[i - 1].second)
                val beforeY = if (i >= 2) yFor(points[i - 2].second) else prevY
                val afterY = if (i < points.lastIndex) yFor(points[i + 1].second) else y
                val lo = min(prevY, y)
                val hi = max(prevY, y)
                val c1y = (prevY + (y - beforeY) / 6f).coerceIn(lo, hi)
                val c2y = (y - (afterY - prevY) / 6f).coerceIn(lo, hi)
                curve.cubicTo(prevX + stepX / 3f, c1y, x - stepX / 3f, c2y, x, y)
            }
        }
        val fill = Path().apply {
            addPath(curve)
            lineTo(xFor(points.lastIndex), plotBottom)
            lineTo(xFor(0), plotBottom)
            close()
        }

        clipRect(left = 0f, top = 0f, right = leftPx + plotWidth * reveal.value + 8.dp.toPx(), bottom = size.height) {
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)),
                    startY = topPx,
                    endY = plotBottom,
                ),
            )
            drawPath(curve, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Dashed average line with a small tag at the right end.
        val averageY = yFor(average)
        drawLine(
            labelColor.copy(alpha = 0.45f),
            Offset(leftPx, averageY),
            Offset(size.width - rightPx, averageY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        )
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        drawContext.canvas.nativeCanvas.drawText("avg " + fmt.format(average), size.width - rightPx, averageY - 6.dp.toPx(), paint)

        // Peak: a hollow marker (no label; the axis carries the scale).
        val peakIndex = points.indices.maxBy { points[it].second }
        if (peakIndex != points.lastIndex && reveal.value > peakIndex.toFloat() / points.lastIndex) {
            drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(xFor(peakIndex), yFor(maxValue)), style = Stroke(2.dp.toPx()))
        }

        // Latest point: halo, solid dot, and a value tag above it, shown once the line has reached it.
        if (reveal.value > 0.97f) {
            val last = points.last().second
            val center = Offset(xFor(points.lastIndex), yFor(last))
            drawCircle(lineColor.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = center)
            drawCircle(lineColor, radius = 5.dp.toPx(), center = center)
            val text = fmt.format(last)
            paint.color = tagTextColor.toArgb()
            paint.isFakeBoldText = true
            paint.textAlign = android.graphics.Paint.Align.CENTER
            val padX = 8.dp.toPx()
            val tagWidth = paint.measureText(text) + padX * 2
            val tagHeight = labelTextSizePx + 10.dp.toPx()
            val tagX = (center.x - tagWidth / 2f).coerceIn(leftPx, size.width - tagWidth)
            val tagY = (center.y - 18.dp.toPx() - tagHeight).coerceAtLeast(0f)
            drawRoundRect(tagColor, Offset(tagX, tagY), Size(tagWidth, tagHeight), CornerRadius(tagHeight / 2f))
            drawContext.canvas.nativeCanvas.drawText(text, tagX + tagWidth / 2f, tagY + tagHeight / 2f + labelTextSizePx * 0.35f, paint)
            paint.isFakeBoldText = false
            paint.color = labelColor.toArgb()
        }

        // First and last dates.
        paint.textAlign = android.graphics.Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText(points.first().first.format(AxisDate), leftPx, size.height - 6.dp.toPx(), paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        drawContext.canvas.nativeCanvas.drawText(points.last().first.format(AxisDate), size.width - rightPx, size.height - 6.dp.toPx(), paint)
    }
}

/**
 * Headline for a line chart panel: the latest value large in the tabular mono face, with a chip showing the
 * change since the first point. [higherIsBetter] colours the chip (green up, amber down); null is neutral, for
 * metrics like body weight where neither direction is "good".
 */
@Composable
fun ChartSummary(
    points: List<Pair<LocalDate, Double>>,
    unit: String,
    modifier: Modifier = Modifier,
    higherIsBetter: Boolean? = true,
) {
    if (points.isEmpty()) return
    val latest = points.last().second
    val delta = latest - points.first().second
    val flat = kotlin.math.abs(delta) < 0.05
    val chipColor = when {
        higherIsBetter == null || flat -> PlateSteel
        (delta > 0) == higherIsBetter -> PlateGreen
        else -> PlateYellow
    }
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "%.1f".format(latest) + " $unit",
            style = LedgerFigureHero.copy(fontSize = 26.sp, lineHeight = 30.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (points.size > 1) {
            Surface(shape = CircleShape, color = chipColor.copy(alpha = 0.16f), modifier = Modifier.padding(bottom = 3.dp)) {
                Text(
                    (if (flat) "no change" else (if (delta > 0) "+" else "-") + "%.1f".format(kotlin.math.abs(delta))) +
                        " since " + points.first().first.format(AxisDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
