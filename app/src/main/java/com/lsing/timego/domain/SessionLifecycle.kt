package com.lsing.timego.domain

const val ONE_HOUR_MILLIS: Long = 60 * 60 * 1000L

enum class SessionAutoCloseDecision { STAY_ACTIVE, AUTO_CLOSE }

/** Decides whether an active session should be auto-closed because it's gone quiet for more than
 *  [inactivityThresholdMillis]. Anchors on [lastSetLoggedAtEpochMillis] (the active session's most
 *  recent SetLog) when one exists; falls back to [sessionStartEpochMillis] for a session with no
 *  sets logged yet, so an empty session opened and abandoned doesn't linger forever and get
 *  silently reused (and mis-dated) the next time the user logs a set. A pure function -- the
 *  caller is responsible for fetching both timestamps and actually writing the resulting end time
 *  via WorkoutRepository.endSession when this returns AUTO_CLOSE. */
fun checkSessionAutoClose(
    lastSetLoggedAtEpochMillis: Long?,
    sessionStartEpochMillis: Long,
    nowEpochMillis: Long,
    inactivityThresholdMillis: Long = ONE_HOUR_MILLIS,
): SessionAutoCloseDecision {
    val anchorEpochMillis = lastSetLoggedAtEpochMillis ?: sessionStartEpochMillis
    return if (nowEpochMillis - anchorEpochMillis > inactivityThresholdMillis) {
        SessionAutoCloseDecision.AUTO_CLOSE
    } else {
        SessionAutoCloseDecision.STAY_ACTIVE
    }
}
