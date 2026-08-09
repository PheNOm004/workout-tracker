package com.lsing.timego.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.diagramZoneIntensity
import com.lsing.timego.domain.heatColor
import com.lsing.timego.domain.heatStopHexes
import com.lsing.timego.domain.parsePathVertices
import com.lsing.timego.domain.recolorByLightness

private data class BuiltMuscleShape(
    val path: Path,
    val muscleGroup: MuscleGroup?,
    val isOutline: Boolean,
    val lightness: Float,
)

private fun buildShapes(specs: List<MusclePathSpec>, viewBox: FloatArray): List<BuiltMuscleShape> {
    val (x0, y0) = viewBox[0] to viewBox[1]
    return specs.map { spec ->
        val vertices = parsePathVertices(spec.pathData)
        val path = Path().apply {
            vertices.firstOrNull()?.let { (x, y) -> moveTo(x - x0, y - y0) }
            vertices.drop(1).forEach { (x, y) -> lineTo(x - x0, y - y0) }
            close()
        }
        BuiltMuscleShape(path, spec.muscleGroup, spec.isOutline, spec.lightness)
    }
}

private fun hexToColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    return Color(r, g, b)
}

/** Front + back anatomy diagram traced from a real muscle-atlas reference (see
 *  docs/superpowers/specs), each of ~176 shapes classified by its position into a [MuscleGroup]
 *  zone (or left neutral for the outline/face/hand/foot detail shapes that aren't a tracked
 *  group). [intensities] maps [MuscleGroup.name] to a 0f..1f normalized recent-volume value, same
 *  shape as [RadarChart]'s input -- a missing key or an explicit 0f both mean genuinely untrained
 *  (there's no other way to get exactly 0f once [intensities] is volume-normalized against the max
 *  group), so those shapes render in the same neutral color as the untracked detail shapes rather
 *  than the heat scale's low end -- otherwise every untrained muscle reads as "trained a little"
 *  cyan, which is misleading. Trained shapes come from [heatColor] (cyan = low, red = high) re-lit
 *  per shape via [recolorByLightness] using that shape's own traced shading, so muscle definition
 *  survives instead of flattening to one flat color per group. A gradient legend renders below the
 *  diagram so the scale is actually readable. */
@Composable
fun MuscleBodyDiagram(intensities: Map<String, Float>, modifier: Modifier = Modifier) {
    // Theme-adaptive mid-grey rather than onSurface (too stark -- white on dark, black on
    // light) or a fixed color (disappears against a same-toned background on one theme).
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val detailColor = MaterialTheme.colorScheme.surfaceVariant

    val frontShapes = remember { buildShapes(FRONT_BODY_PATHS, FRONT_BODY_VIEWBOX) }
    val backShapes = remember { buildShapes(BACK_BODY_PATHS, BACK_BODY_VIEWBOX) }
    val frontAspect = (FRONT_BODY_VIEWBOX[2] - FRONT_BODY_VIEWBOX[0]) / (FRONT_BODY_VIEWBOX[3] - FRONT_BODY_VIEWBOX[1])
    val backAspect = (BACK_BODY_VIEWBOX[2] - BACK_BODY_VIEWBOX[0]) / (BACK_BODY_VIEWBOX[3] - BACK_BODY_VIEWBOX[1])

    fun colorFor(shape: BuiltMuscleShape): Color = when {
        shape.isOutline -> outlineColor
        shape.muscleGroup != null -> {
            val intensity = diagramZoneIntensity(shape.muscleGroup, intensities)
            if (intensity <= 0f) {
                detailColor
            } else {
                hexToColor(recolorByLightness(heatColor(intensity), shape.lightness))
            }
        }
        else -> detailColor
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.weight(1f).aspectRatio(frontAspect)) {
                val scaleFactor = size.width / (FRONT_BODY_VIEWBOX[2] - FRONT_BODY_VIEWBOX[0])
                scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                    frontShapes.forEach { drawPath(it.path, color = colorFor(it)) }
                }
            }
            Canvas(modifier = Modifier.weight(1f).aspectRatio(backAspect)) {
                val scaleFactor = size.width / (BACK_BODY_VIEWBOX[2] - BACK_BODY_VIEWBOX[0])
                scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                    backShapes.forEach { drawPath(it.path, color = colorFor(it)) }
                }
            }
        }
        HeatLegend(detailColor = detailColor, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun HeatLegend(detailColor: Color, modifier: Modifier = Modifier) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gradientColors = remember { heatStopHexes().map(::hexToColor) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(detailColor))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Untrained", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("Low", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(gradientColors)),
            )
            Text("High", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}
