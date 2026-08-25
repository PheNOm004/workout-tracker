package com.lsing.timego.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Plain Canvas line chart -- no charting library dependency, same "draw it yourself" approach
 *  HeatP used for its WeeklyBarChart. A dashed average-value reference line (sparkline style,
 *  inspired by mobile strength-tracking apps' per-muscle trend rows) gives the curve context
 *  without needing full axis chrome; first/last date labels and the average value anchor it
 *  further. Padding keeps the end points and labels from clipping against the canvas edge.
 *  Shared by the Progress screen's strength curve and body-metric (weight) curve. */
@Composable
fun SparklineChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 12.sp.toPx() }
    val horizontalPaddingPx = with(density) { 8.dp.toPx() }
    val topPaddingPx = with(density) { 12.dp.toPx() }
    val bottomPaddingPx = with(density) { 28.dp.toPx() }

    Canvas(modifier = modifier) {
        if (points.isEmpty()) {
            return@Canvas
        }
        if (points.size == 1) {
            // A single data point (e.g. every entry so far falls on one calendar day) can't draw
            // a line, but showing nothing reads as a bug, not "not enough data yet" -- draw the
            // one point plus its value so there's something to see.
            val (date, value) = points.first()
            val y = size.height / 2f
            drawCircle(color = lineColor, radius = 6f, center = Offset(size.width / 2f, y))
            val nativePaint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                "$date: %.1f".format(value),
                size.width / 2f,
                y - 16f,
                nativePaint,
            )
            return@Canvas
        }
        val maxValue = points.maxOf { it.second }
        val minValue = points.minOf { it.second }
        val average = points.map { it.second }.average()
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val plotWidth = size.width - horizontalPaddingPx * 2
        val plotHeight = size.height - topPaddingPx - bottomPaddingPx
        val stepX = plotWidth / (points.size - 1)

        fun xFor(index: Int) = horizontalPaddingPx + stepX * index
        fun yFor(value: Double) = topPaddingPx + plotHeight - ((value - minValue) / range * plotHeight).toFloat()

        val path = Path()
        points.forEachIndexed { index, (_, value) ->
            val x = xFor(index)
            val y = yFor(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val plotBottom = topPaddingPx + plotHeight
        val filledPath = Path().apply {
            addPath(path)
            lineTo(xFor(points.lastIndex), plotBottom)
            lineTo(xFor(0), plotBottom)
            close()
        }
        drawPath(
            path = filledPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.24f), lineColor.copy(alpha = 0.02f)),
                startY = topPaddingPx,
                endY = plotBottom,
            ),
        )

        val averageY = yFor(average)
        drawLine(
            color = labelColor.copy(alpha = 0.4f),
            start = Offset(horizontalPaddingPx, averageY),
            end = Offset(size.width - horizontalPaddingPx, averageY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { index, (_, value) ->
            drawCircle(color = lineColor, radius = 5f, center = Offset(xFor(index), yFor(value)))
        }

        val nativePaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSizePx
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.apply {
            nativePaint.textAlign = android.graphics.Paint.Align.LEFT
            drawText(points.first().first.toString(), horizontalPaddingPx, size.height - 8f, nativePaint)
            nativePaint.textAlign = android.graphics.Paint.Align.RIGHT
            drawText(points.last().first.toString(), size.width - horizontalPaddingPx, size.height - 8f, nativePaint)
            nativePaint.textAlign = android.graphics.Paint.Align.LEFT
            drawText("avg %.1f".format(average), horizontalPaddingPx, averageY - 8f, nativePaint)

            fun drawTag(text: String, index: Int, y: Float, align: android.graphics.Paint.Align) {
                nativePaint.textAlign = align
                val labelY = (y - 8f).coerceAtLeast(topPaddingPx + labelTextSizePx)
                drawText(text, xFor(index), labelY, nativePaint)
            }

            // min/max/now can share the same point (e.g. the latest set is also the heaviest),
            // which used to draw two labels stacked on top of each other. Group by index and
            // combine into one "max / now 82.5" tag instead so nothing overlaps.
            val minIndex = points.indices.minBy { points[it].second }
            val maxIndex = points.indices.maxBy { points[it].second }
            val nowIndex = points.lastIndex
            val tagsByIndex = linkedMapOf<Int, MutableList<String>>()
            tagsByIndex.getOrPut(minIndex) { mutableListOf() }.add("min")
            tagsByIndex.getOrPut(maxIndex) { mutableListOf() }.add("max")
            tagsByIndex.getOrPut(nowIndex) { mutableListOf() }.add("now")
            tagsByIndex.forEach { (index, labels) ->
                val text = "${labels.joinToString(" / ")} %.1f".format(points[index].second)
                val align = if (index == 0) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                drawTag(text, index, yFor(points[index].second), align)
            }
        }
    }
}
