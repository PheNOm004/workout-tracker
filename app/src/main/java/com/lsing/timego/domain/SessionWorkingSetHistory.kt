package com.lsing.timego.domain

import com.lsing.timego.data.SetLog

/** Reduces raw sets for one exercise into one representative set per session -- the session's
 *  last non-warmup set, which is that session's ending effort. Ordered oldest-session-first.
 *  Warmup sets ([SetLog.isWarmup]) are excluded entirely: they never count toward the working-set
 *  baseline or the plateau/trend window. [sessionStartById] resolves chronological session order
 *  (sessions aren't necessarily ordered by id once multiple sessions can land on the same date). */
fun sessionWorkingSetHistory(setLogs: List<SetLog>, sessionStartById: Map<Long, Long>): List<SetLog> {
    val latestBySession = mutableMapOf<Long, SetLog>()
    for (set in setLogs) {
        if (set.isWarmup) continue
        val latest = latestBySession[set.sessionId]
        if (latest == null || set.loggedAtEpochMillis > latest.loggedAtEpochMillis) {
            latestBySession[set.sessionId] = set
        }
    }
    return latestBySession.values.sortedBy { sessionStartById[it.sessionId] ?: 0L }
}
