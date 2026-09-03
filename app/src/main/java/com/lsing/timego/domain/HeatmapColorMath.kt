package com.lsing.timego.domain

/** Small hex/HSV colour toolkit -- plain Kotlin, no android.graphics.Color dependency, so it
 *  stays unit-testable without Robolectric. `internal` (not private) so MuscleHeatColor.kt can
 *  reuse it rather than duplicating the same "hex string in, hex string out" math. */
internal fun hexToRgb(hex: String): Triple<Int, Int, Int> {
    val clean = hex.removePrefix("#")
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    return Triple(r, g, b)
}

internal fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / delta).mod(6f))
        max == gf -> 60f * (((bf - rf) / delta) + 2f)
        else -> 60f * (((rf - gf) / delta) + 4f)
    }
    val s = if (max == 0f) 0f else delta / max
    return Triple(h, s, max)
}

internal fun hsvToHex(h: Float, s: Float, v: Float): String {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f).mod(2f) - 1))
    val m = v - c
    val (rp, gp, bp) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun toByte(component: Float) = ((component + m) * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(toByte(rp), toByte(gp), toByte(bp))
}
