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

/** The program's prescribed rest (midpoint of its range) when there is one, else the user's default. */
fun restSecondsFor(prescription: Prescription?, defaultSeconds: Int): Int =
    prescription?.let { (it.restMinSec + it.restMaxSec) / 2 } ?: defaultSeconds

/** "2:05", or "+0:12" once over. */
fun formatRestClock(remainingSeconds: Int): String {
    val abs = kotlin.math.abs(remainingSeconds)
    return "${if (remainingSeconds < 0) "+" else ""}${abs / 60}:${"%02d".format(abs % 60)}"
}

const val MIN_REST_SECONDS = 5
const val DEFAULT_REST_SECONDS = 120
