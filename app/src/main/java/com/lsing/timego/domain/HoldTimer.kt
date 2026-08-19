package com.lsing.timego.domain

/** Drives the live hold/cardio timer -- absent (null) represents idle, not part of this sealed
 *  class, since idle has no elapsed time of its own. */
sealed class HoldTimerPhase {
    data class CountingDown(val secondsRemaining: Int) : HoldTimerPhase()
    data class Running(val elapsedSeconds: Int) : HoldTimerPhase()
}

/** Resolves the timer's phase from wall-clock timestamps rather than by counting ticks.
 *
 *  The previous model advanced one second per `delay(1000)` + recomposition, so every cycle cost
 *  slightly more than a second and the error accumulated -- and because the displayed elapsed
 *  value is what gets persisted as the set's duration, that drift was written into the logged
 *  data, always short. Deriving from [startedAtEpochMillis] means a late or coalesced tick
 *  corrects itself on the next pass instead of permanently losing time.
 *
 *  The countdown rounds up so a freshly started 5-second delay reads "5s" rather than "4s". */
fun timerPhaseAt(startedAtEpochMillis: Long, delaySeconds: Int, nowEpochMillis: Long): HoldTimerPhase {
    val elapsedMillis = (nowEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)
    val delayMillis = delaySeconds.coerceAtLeast(0) * 1000L
    return if (elapsedMillis < delayMillis) {
        HoldTimerPhase.CountingDown(secondsRemaining = ((delayMillis - elapsedMillis + 999) / 1000).toInt())
    } else {
        HoldTimerPhase.Running(elapsedSeconds = ((elapsedMillis - delayMillis) / 1000).toInt())
    }
}
