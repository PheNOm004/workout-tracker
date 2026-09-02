package com.lsing.timego.domain

import com.lsing.timego.data.SetLog

data class RepRange(val floor: Int, val ceiling: Int)

private const val MIN_SESSIONS_FOR_RANGE = 3

/** Derives a working rep range for [weightKg] from every past working set logged at exactly that
 *  weight, requiring sets from at least [MIN_SESSIONS_FOR_RANGE] distinct sessions before returning
 *  a range -- a few sets within one session reflect within-session fatigue, not a real range the
 *  lifter operates in at this weight. Returns null (not enough history) below that bar, including
 *  immediately after every weight escalation, when by definition no history exists at the new
 *  weight yet -- callers fall back to single-targetReps behavior in that case. Warmup sets are
 *  excluded, same convention as [sessionWorkingSetHistory]. */
fun repRangeAtWeight(allWorkingSets: List<SetLog>, weightKg: Double): RepRange? {
    val distinctSessions = mutableSetOf<Long>()
    var minReps = Int.MAX_VALUE
    var maxReps = Int.MIN_VALUE
    for (set in allWorkingSets) {
        if (set.isWarmup || set.weightKg != weightKg) continue
        distinctSessions += set.sessionId
        if (set.reps < minReps) minReps = set.reps
        if (set.reps > maxReps) maxReps = set.reps
    }
    if (distinctSessions.size < MIN_SESSIONS_FOR_RANGE) return null
    return RepRange(floor = minReps, ceiling = maxReps)
}
