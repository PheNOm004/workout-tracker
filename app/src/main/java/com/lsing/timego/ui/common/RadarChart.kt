package com.lsing.timego.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Spider/radar chart -- pure geometry (N axes spaced evenly by angle, a value polygon, a couple
 *  of reference rings), no vector art needed, unlike an anatomical muscle-diagram illustration.
 *  [values] maps an axis label to a 0f..1f normalized magnitude (the caller decides what "1f"
 *  means, e.g. divide every muscle group's volume by the single highest one). Axis order follows
 *  the map's iteration order -- pass a LinkedHashMap or similar if order matters to the caller. */
@Composable
fun RadarChart(values: Map<String, Float>, modifier: Modifier = Modifier) {
    val fillColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 12.sp.toPx() }

    Canvas(modifier = modifier) {
        val axisCount = values.size
        if (axisCount < 3) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = min(size.width, size.height) / 2f - labelTextSizePx * 2

        fun pointFor(index: Int, radiusFraction: Float): Offset {
            val angle = (-Math.PI / 2) + (2 * Math.PI * index / axisCount)
            return Offset(
                center.x + (maxRadius * radiusFraction * cos(angle)).toFloat(),
                center.y + (maxRadius * radiusFraction * sin(angle)).toFloat(),
            )
        }

        // Reference rings at 1/3 and 2/3 radius, plus the outer axis lines.
        listOf(0.33f, 0.66f, 1f).forEach { ringFraction ->
            val ringPath = Path()
            for (i in 0 until axisCount) {
                val point = pointFor(i, ringFraction)
                if (i == 0) ringPath.moveTo(point.x, point.y) else ringPath.lineTo(point.x, point.y)
            }
            ringPath.close()
            drawPath(ringPath, color = gridColor.copy(alpha = 0.25f), style = Stroke(width = 1.5f))
        }
        for (i in 0 until axisCount) {
            drawLine(color = gridColor.copy(alpha = 0.25f), start = center, end = pointFor(i, 1f), strokeWidth = 1.5f)
        }

        // The actual value polygon.
        val valuePath = Path()
        values.values.forEachIndexed { index, value ->
            val point = pointFor(index, value.coerceIn(0f, 1f))
            if (index == 0) valuePath.moveTo(point.x, point.y) else valuePath.lineTo(point.x, point.y)
        }
        valuePath.close()
        drawPath(valuePath, color = fillColor.copy(alpha = 0.35f))
        drawPath(valuePath, color = fillColor, style = Stroke(width = 3f))

        val nativePaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSizePx
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        values.keys.forEachIndexed { index, label ->
            val labelPoint = pointFor(index, 1.18f)
            drawContext.canvas.nativeCanvas.drawText(label, labelPoint.x, labelPoint.y, nativePaint)
        }
    }
}
