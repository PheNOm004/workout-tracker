package com.lsing.timego.domain

import kotlin.math.roundToInt

/** Night Training Console intensity scale for low-to-high recent volume. The neutral low end
 * recedes into the dark UI, mint communicates building momentum, amber signals attention, and
 * coral marks the hottest training load. [intensity] (0..1, clamped) is interpolated linearly
 * between whichever pair of stops it falls between. Plain Kotlin/hex-string I/O keeps this
 * domain helper unit-testable without Robolectric. */
private val HEAT_STOPS = listOf(
    "#30383B", // low -- neutral surface
    "#4A6A5D", // building momentum
    "#9BD8B2", // positive training signal
    "#F2B866", // attention / high load
    "#FF6B5E", // high -- brand coral
)

/** The scale's stops in order, low to high -- exposed so UI code can render a matching legend
 * without duplicating this list. */
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
 * saturation, so each traced muscle shape retains its original light/shadow definition. */
fun recolorByLightness(baseHex: String, targetLightness: Float): String {
    val (r, g, b) = hexToRgb(baseHex)
    val (h, s, _) = rgbToHsv(r, g, b)
    return hsvToHex(h, s.coerceAtLeast(0.55f), targetLightness.coerceIn(0f, 1f))
}
