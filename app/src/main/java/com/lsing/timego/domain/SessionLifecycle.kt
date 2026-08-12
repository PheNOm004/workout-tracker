package com.lsing.timego.domain

const val ONE_HOUR_MILLIS: Long = 60 * 60 * 1000L

enum class SessionAutoCloseDecision { STAY_ACTIVE, AUTO_CLOSE }

/** Decides whether an active session should be auto-closed because its last logged set is more
 *  than [inactivityThresholdMillis] in the past. A pure function -- the caller is responsible for
 *  fetching [lastSetLoggedAtEpochMillis] (the active session's most recent SetLog) and actually
 *  writing the resulting end time via WorkoutRepository.endSession when this returns AUTO_CLOSE. */
fun checkSessionAutoClose(
    lastSetLoggedAtEpochMillis: Long,
    nowEpochMillis: Long,
    inactivityThresholdMillis: Long = ONE_HOUR_MILLIS,
): SessionAutoCloseDecision =
    if (nowEpochMillis - lastSetLoggedAtEpochMillis > inactivityThresholdMillis) {
        SessionAutoCloseDecision.AUTO_CLOSE
    } else {
        SessionAutoCloseDecision.STAY_ACTIVE
    }
