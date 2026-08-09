package com.lsing.timego.domain

import kotlin.math.roundToInt

/** Cyan-to-red intensity scale for the muscle heatmap (low recent volume -> high). Matches the
 *  legend agreed on during design: cyan, blue, violet, pink, red as evenly-spaced stops, with
 *  [intensity] (0..1, clamped) interpolated linearly between whichever pair of stops it falls
 *  between. Deliberately plain Kotlin/hex-string in-out, same style as [habitHeatmapColorHexes],
 *  so it stays unit-testable without Robolectric and callers convert to Compose Color at the UI
 *  edge. */
private val HEAT_STOPS = listOf(
    "#22D3EE", // low
    "#3B82F6",
    "#8B5CF6",
    "#EC4899",
    "#EF4444", // high
)

/** The scale's stops in order, low to high -- exposed so UI code can render a matching legend
 *  without duplicating this list. */
fun heatStopHexes(): List<String> = HEAT_STOPS

fun heatColor(intensity: Float): String {
    val clamped = intensity.coerceIn(0f, 1f)
    val span = clamped * (HEAT_STOPS.size - 1)
    val lowIndex = span.toInt().coerceIn(0, HEAT_STOPS.size - 2)
    val t = span - lowIndex
    return lerpHex(HEAT_STOPS[lowIndex], HEAT_STOPS[lowIndex + 1], t)
}

private fun lerpHex(fromHex: String, toHex: String, t: Float): String {
    val (r1, g1, b1) = hexToRgb(fromHex)
    val (r2, g2, b2) = hexToRgb(toHex)
    fun lerp(a: Int, b: Int) = (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(lerp(r1, r2), lerp(g1, g2), lerp(b1, b2))
}

/** Recolors [baseHex] to [targetLightness] (0..1, HSV value) while keeping its hue and a floor
 *  saturation, so a muscle group's heat color can be re-rendered once per traced path shape using
 *  that shape's own original shading value ([MusclePathSpec.lightness]) -- this is what preserves
 *  the source artwork's light/shadow muscle definition instead of flattening a whole muscle group
 *  to one flat fill. */
fun recolorByLightness(baseHex: String, targetLightness: Float): String {
    val (r, g, b) = hexToRgb(baseHex)
    val (h, s, _) = rgbToHsv(r, g, b)
    return hsvToHex(h, s.coerceAtLeast(0.55f), targetLightness.coerceIn(0f, 1f))
}
