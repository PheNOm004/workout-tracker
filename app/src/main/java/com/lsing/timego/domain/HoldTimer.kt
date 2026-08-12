package com.lsing.timego.domain

/** Drives the live hold-exercise timer on `HoldLogRow` -- absent (null) represents idle, not part
 *  of this sealed class, since idle has no tick behavior of its own. */
sealed class HoldTimerPhase {
    data class CountingDown(val secondsRemaining: Int) : HoldTimerPhase()
    data class Running(val elapsedSeconds: Int) : HoldTimerPhase()
}

/** Called once per second while a timer is active. [HoldTimerPhase.CountingDown] decrements until
 *  it would hit zero, then jumps straight to [HoldTimerPhase.Running] starting at 0 elapsed --
 *  there's no "0 seconds remaining" tick shown, the count-up begins immediately. */
fun HoldTimerPhase.tick(): HoldTimerPhase = when (this) {
    is HoldTimerPhase.CountingDown ->
        if (secondsRemaining <= 1) HoldTimerPhase.Running(elapsedSeconds = 0) else HoldTimerPhase.CountingDown(secondsRemaining - 1)
    is HoldTimerPhase.Running -> HoldTimerPhase.Running(elapsedSeconds + 1)
}
