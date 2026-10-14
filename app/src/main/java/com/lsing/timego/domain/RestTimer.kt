package com.lsing.timego.domain

import com.lsing.timego.data.Prescription

/** A running rest period. Wall-clock based so it survives recomposition and keeps counting while the
 *  screen is off; [totalSeconds] is adjustable mid-rest. */
data class RestTimerState(val startedAtMillis: Long, val totalSeconds: Int)

/** Whole seconds left; negative once the rest has run over. */
fun RestTimerState.remainingSeconds(nowMillis: Long): Int =
    totalSeconds - ((nowMillis - startedAtMillis) / 1000L).toInt()

fun RestTimerState.adjusted(deltaSeconds: Int): RestTimerState =
    copy(totalSeconds = (totalSeconds + deltaSeconds).coerceAtLeast(MIN_REST_SECONDS))

/** Rest for the set just logged. Static: always [defaultSeconds]. Adaptive: the program's prescribed rest
 *  (midpoint of its range) when there is one, else [defaultSeconds], then nudged by how hard the set was:
 *  RPE 9+ rests 30s longer, RPE 6 or lower 15s shorter. */
fun restSecondsFor(prescription: Prescription?, defaultSeconds: Int, adaptive: Boolean = true, lastRpe: Int? = null): Int {
    if (!adaptive) return defaultSeconds
    val base = prescription?.let { (it.restMinSec + it.restMaxSec) / 2 } ?: defaultSeconds
    val nudge = when {
        lastRpe == null -> 0
        lastRpe >= 9 -> 30
        lastRpe <= 6 -> -15
        else -> 0
    }
    return (base + nudge).coerceAtLeast(MIN_REST_SECONDS)
}

/** "2:05", or "+0:12" once over. */
fun formatRestClock(remainingSeconds: Int): String {
    val abs = kotlin.math.abs(remainingSeconds)
    return "${if (remainingSeconds < 0) "+" else ""}${abs / 60}:${"%02d".format(abs % 60)}"
}

const val MIN_REST_SECONDS = 5
const val DEFAULT_REST_SECONDS = 120
