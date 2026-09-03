package com.lsing.timego.ui.common

import android.graphics.Region
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.theme.TimeGoMotion
import com.lsing.timego.domain.MuscleSetSummary
import com.lsing.timego.domain.boundingBox
import com.lsing.timego.domain.diagramGroupsForHeatmap
import com.lsing.timego.domain.diagramZoneIntensity
import com.lsing.timego.domain.heatColor
import com.lsing.timego.domain.heatStopHexes
import com.lsing.timego.domain.parsePathVertices
import com.lsing.timego.domain.recolorByLightness
import com.lsing.timego.ui.theme.Spacing

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

/** One hit-test region per muscle group, in the same viewBox-local coordinate space [buildShapes]
 *  produces (origin subtracted) -- a long-press position is mapped back into this space by dividing
 *  by the canvas scale factor, then tested against each region. The outline and neutral detail
 *  shapes are skipped: they carry no muscle group. */
private fun buildGroupRegions(shapes: List<BuiltMuscleShape>, viewBox: FloatArray): Map<MuscleGroup, Region> {
    val clip = Region(
        0,
        0,
        (viewBox[2] - viewBox[0]).toInt() + 1,
        (viewBox[3] - viewBox[1]).toInt() + 1,
    )
    return shapes
        .filter { !it.isOutline && it.muscleGroup != null }
        .groupBy { it.muscleGroup!! }
        .mapValues { (_, groupShapes) ->
            Region().apply {
                groupShapes.forEach { shape ->
                    op(Region().apply { setPath(shape.path.asAndroidPath(), clip) }, Region.Op.UNION)
                }
            }
        }
}

private fun regionHitAt(regions: Map<MuscleGroup, Region>, position: Offset, scaleFactor: Float): MuscleGroup? {
    if (scaleFactor <= 0f) return null
    val x = (position.x / scaleFactor).toInt()
    val y = (position.y / scaleFactor).toInt()
    return regions.entries.firstOrNull { it.value.contains(x, y) }?.key
}

/** Long-press a muscle zone to raise its readout, release to drop it -- [onHold] fires with the
 *  hit group on a long press and with null once the finger lifts or the gesture is cancelled. */
private fun Modifier.muscleHoldGesture(
    regions: Map<MuscleGroup, Region>,
    viewBox: FloatArray,
    onHold: (MuscleGroup?) -> Unit,
): Modifier = pointerInput(regions) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        val scaleFactor = this@pointerInput.size.width / (viewBox[2] - viewBox[0])
        val hit = regionHitAt(regions, longPress.position, scaleFactor) ?: return@awaitEachGesture
        onHold(hit)
        do {
            val event = awaitPointerEvent()
        } while (event.changes.any { it.pressed })
        onHold(null)
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
 *  green, which is misleading. Trained shapes come from [heatColor] (neutral/mint = low, coral = high) re-lit
 *  per shape via [recolorByLightness] using that shape's own traced shading, so muscle definition
 *  survives instead of flattening to one flat color per group. A gradient legend renders below the
 *  diagram so the scale is actually readable. */
@Composable
fun MuscleBodyDiagram(
    intensities: Map<String, Float>,
    modifier: Modifier = Modifier,
    periodLabel: String = "this period",
    setSummaries: Map<String, MuscleSetSummary> = emptyMap(),
) {
    // Theme-adaptive mid-grey rather than onSurface (too stark -- white on dark, black on
    // light) or a fixed color (disappears against a same-toned background on one theme).
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val detailColor = MaterialTheme.colorScheme.surfaceVariant

    val frontShapes = remember { buildShapes(FRONT_BODY_PATHS, FRONT_BODY_VIEWBOX) }
    val backShapes = remember { buildShapes(BACK_BODY_PATHS, BACK_BODY_VIEWBOX) }
    val frontRegions = remember(frontShapes) { buildGroupRegions(frontShapes, FRONT_BODY_VIEWBOX) }
    val backRegions = remember(backShapes) { buildGroupRegions(backShapes, BACK_BODY_VIEWBOX) }
    var pressedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    val frontAspect = (FRONT_BODY_VIEWBOX[2] - FRONT_BODY_VIEWBOX[0]) / (FRONT_BODY_VIEWBOX[3] - FRONT_BODY_VIEWBOX[1])
    val backAspect = (BACK_BODY_VIEWBOX[2] - BACK_BODY_VIEWBOX[0]) / (BACK_BODY_VIEWBOX[3] - BACK_BODY_VIEWBOX[1])

    val animatedGroupIntensities = MuscleGroup.entries.associateWith { group ->
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = diagramZoneIntensity(group, intensities),
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.85f,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
            ),
            label = "groupIntensity_${group.name}",
        ).value
    }

    fun colorFor(shape: BuiltMuscleShape): Color = when {
        shape.isOutline -> outlineColor
        shape.muscleGroup != null -> {
            val intensity = animatedGroupIntensities[shape.muscleGroup] ?: 0f
            if (intensity <= 0.01f) {
                detailColor
            } else {
                hexToColor(recolorByLightness(heatColor(intensity), shape.lightness))
            }
        }
        else -> detailColor
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(frontAspect)
                    .muscleHoldGesture(frontRegions, FRONT_BODY_VIEWBOX) { pressedGroup = it },
            ) {
                val scaleFactor = size.width / (FRONT_BODY_VIEWBOX[2] - FRONT_BODY_VIEWBOX[0])
                scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                    frontShapes.forEach { drawPath(it.path, color = colorFor(it)) }
                }
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(backAspect)
                    .muscleHoldGesture(backRegions, BACK_BODY_VIEWBOX) { pressedGroup = it },
            ) {
                val scaleFactor = size.width / (BACK_BODY_VIEWBOX[2] - BACK_BODY_VIEWBOX[0])
                scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                    backShapes.forEach { drawPath(it.path, color = colorFor(it)) }
                }
            }
        }
        HeatLegend(
            detailColor = detailColor,
            periodLabel = periodLabel,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }

    // Kept mounted through the exit animation: targetState follows the hold, currentState lags
    // until the fade/scale-out finishes. shownGroup holds the last group so the card still has
    // content to render while it animates away after the finger lifts.
    val popupVisible = remember { MutableTransitionState(false) }
    popupVisible.targetState = pressedGroup != null
    var shownGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    pressedGroup?.let { shownGroup = it }

    if (popupVisible.currentState || popupVisible.targetState) {
        Popup(alignment = Alignment.Center) {
            AnimatedVisibility(
                visibleState = popupVisible,
                enter = fadeIn(TimeGoMotion.contentEnter) + scaleIn(TimeGoMotion.contentEnter, initialScale = 0.9f),
                exit = fadeOut(TimeGoMotion.contentExit) + scaleOut(TimeGoMotion.contentExit, targetScale = 0.9f),
            ) {
                shownGroup?.let { group ->
                    MuscleSetSummaryCard(group, setSummaries[group.name], periodLabel)
                }
            }
        }
    }
}

/** Compact hold-to-peek readout for one muscle zone: the hardest set that hit it this period
 *  (weight and reps as logged) plus how many sets did. */
@Composable
private fun MuscleSetSummaryCard(group: MuscleGroup, summary: MuscleSetSummary?, periodLabel: String) {
    SurfaceCard(modifier = Modifier.widthIn(max = 240.dp), hero = true) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(formatEnumLabel(group.name), style = MaterialTheme.typography.labelLarge)
            if (summary == null) {
                Text(
                    "No sets · $periodLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.ExtraSmall),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    MiniStat("Weight", "%.1f kg".format(summary.bestWeightKg), Modifier.weight(1f))
                    MiniStat("Reps", summary.bestReps.toString(), Modifier.weight(1f))
                    MiniStat("Sets", summary.setCount.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Renders only the anatomy shapes belonging to [muscleGroups], each half (front/back) cropped
 *  tight to a bounding box around just those shapes instead of the full-body viewBox -- so a
 *  session that only worked chest and triceps shows a compact chest+triceps cutout rather than a
 *  mostly-empty full silhouette. A half with no matching shapes is omitted entirely (not rendered
 *  as blank space) so e.g. an all-front-body session doesn't reserve dead width for an empty back
 *  canvas. With [intensities], the crop uses the same relative heat scale as the Progress tab;
 *  without it, [accentColor] is shaded per shape as a binary "in/out of the set" signal. */
@Composable
fun CroppedMuscleDiagram(
    muscleGroups: Set<String>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    intensities: Map<String, Float> = emptyMap(),
    highlightGroups: Set<String> = muscleGroups,
    neutralizeUnhighlighted: Boolean = false,
    emptyLabel: String = "Nothing yet",
) {
    val drawableGroups = remember(muscleGroups) { diagramGroupsForHeatmap(muscleGroups) }
    val drawableHighlightGroups = remember(highlightGroups) { diagramGroupsForHeatmap(highlightGroups) }
    val frontSpecs = remember(drawableGroups) {
        FRONT_BODY_PATHS.filter { it.muscleGroup != null && it.muscleGroup.name in drawableGroups }
    }
    val backSpecs = remember(drawableGroups) {
        BACK_BODY_PATHS.filter { it.muscleGroup != null && it.muscleGroup.name in drawableGroups }
    }
    if (frontSpecs.isEmpty() && backSpecs.isEmpty()) {
        Text(emptyLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        return
    }

    // Height-capped by the caller (e.g. .heightIn(max = 148.dp)) -- each half sizes its own width
    // from that height via aspect ratio (matchHeightConstraintsFirst) rather than splitting the
    // full row width evenly, since an even split combined with a tall/narrow crop (e.g. calves)
    // was blowing past the row's height entirely: Compose doesn't clip a child that overflows its
    // parent's bounds, so it visually spilled into the sections below instead of shrinking.
    //
    // A session that only highlights a small region (e.g. one muscle group) has a narrow combined
    // aspect ratio, so rendering it at the full height cap left it tiny and floating in a mostly
    // empty row. Instead measure the available width up front and shrink the row's height so the
    // diagram's natural width actually fills it, down to a legibility floor.
    val frontAspect = remember(frontSpecs) { cropAspect(frontSpecs) }
    val backAspect = remember(backSpecs) { cropAspect(backSpecs) }
    val halfCount = listOfNotNull(frontAspect, backAspect).size
    val totalAspect = (frontAspect ?: 0f) + (backAspect ?: 0f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val spacingPx = with(density) { Spacing.Medium.toPx() } * (halfCount - 1).coerceAtLeast(0)
        val availableWidthPx = with(density) { maxWidth.toPx() } - spacingPx
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val minHeightPx = with(density) { 64.dp.toPx() }
        val resolvedHeight = with(density) {
            if (totalAspect <= 0f || maxHeightPx <= 0f) {
                maxHeight
            } else {
                (availableWidthPx / totalAspect).coerceIn(minHeightPx, maxHeightPx).toDp()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(resolvedHeight),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterHorizontally),
        ) {
            if (frontSpecs.isNotEmpty()) {
                CroppedMuscleHalf(
                    specs = frontSpecs,
                    highlightGroups = drawableHighlightGroups,
                    neutralizeUnhighlighted = neutralizeUnhighlighted,
                    accentColor = accentColor,
                    intensities = intensities,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            if (backSpecs.isNotEmpty()) {
                CroppedMuscleHalf(
                    specs = backSpecs,
                    highlightGroups = drawableHighlightGroups,
                    neutralizeUnhighlighted = neutralizeUnhighlighted,
                    accentColor = accentColor,
                    intensities = intensities,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}

private fun cropAspect(specs: List<MusclePathSpec>): Float? {
    if (specs.isEmpty()) return null
    val vertexLists = specs.map { parsePathVertices(it.pathData) }
    val box = boundingBox(vertexLists, padding = 20f) ?: return null
    return ((box[2] - box[0]) / (box[3] - box[1])).coerceAtLeast(0.05f)
}

private data class CroppedMuscleShape(val path: Path, val lightness: Float, val muscleGroup: MuscleGroup?)

@Composable
private fun CroppedMuscleHalf(
    specs: List<MusclePathSpec>,
    highlightGroups: Set<String>,
    neutralizeUnhighlighted: Boolean,
    accentColor: Color,
    intensities: Map<String, Float>,
    modifier: Modifier = Modifier,
) {
    val vertexLists = remember(specs) { specs.map { parsePathVertices(it.pathData) } }
    val cropBox = remember(vertexLists) { boundingBox(vertexLists, padding = 20f) }

    if (cropBox == null) return

    val shapes = remember(specs, cropBox) {
        val (x0, y0) = cropBox[0] to cropBox[1]
        specs.indices.map { i ->
            val path = Path().apply {
                vertexLists[i].firstOrNull()?.let { (x, y) -> moveTo(x - x0, y - y0) }
                vertexLists[i].drop(1).forEach { (x, y) -> lineTo(x - x0, y - y0) }
                close()
            }
            CroppedMuscleShape(path, specs[i].lightness, specs[i].muscleGroup)
        }
    }
    val aspect = ((cropBox[2] - cropBox[0]) / (cropBox[3] - cropBox[1])).coerceAtLeast(0.05f)

    Canvas(modifier = modifier.aspectRatio(aspect, matchHeightConstraintsFirst = true)) {
        val scaleFactor = size.height / (cropBox[3] - cropBox[1])
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
            shapes.forEach { shape ->
                val isHighlighted = shape.muscleGroup?.name in highlightGroups
                // Context-only groups (e.g. chest/biceps pulled in just to size an upper-body
                // recommendation crop) size the crop box but aren't drawn -- otherwise muscles
                // that weren't actually recommended show up fully rendered next to the ones that
                // were, misleadingly widening what looks "recommended".
                if (neutralizeUnhighlighted && !isHighlighted) return@forEach
                val intensity = shape.muscleGroup?.name?.let(intensities::get)
                val color = if (intensity != null && intensity > 0f) {
                    hexToColor(recolorByLightness(heatColor(intensity), shape.lightness))
                } else {
                    accentColor.copy(alpha = (0.55f + shape.lightness * 0.45f).coerceIn(0.45f, 1f))
                }
                drawPath(shape.path, color = color)
            }
        }
    }
}

@Composable
private fun HeatLegend(detailColor: Color, periodLabel: String, modifier: Modifier = Modifier) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gradientColors = remember { heatStopHexes().map(::hexToColor) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(detailColor))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Untrained · $periodLabel", style = MaterialTheme.typography.labelSmall, color = labelColor)
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

/** Same heat-scale key used by the full Progress diagram, exposed for compact diagrams that
 *  appear elsewhere in the app. Keeping the key shared prevents the landing page from making
 *  the same colors mean something different from Progress. */
@Composable
fun MuscleHeatLegend(detailColor: Color, periodLabel: String, modifier: Modifier = Modifier) {
    HeatLegend(detailColor = detailColor, periodLabel = periodLabel, modifier = modifier)
}
