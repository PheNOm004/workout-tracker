package com.lsing.timego.domain

import com.lsing.timego.data.WorkoutSession

/** A finished session is still "the same workout" for this long: forgetting an exercise and coming back
 *  within the window should extend it, not start a second one. */
const val SESSION_CONTINUE_WINDOW_MS = 4 * 60 * 60 * 1000L

/** The session to offer to continue instead of starting a new one: the most recently ended session for
 *  the same routine today, finished within [SESSION_CONTINUE_WINDOW_MS]. */
fun continuableSession(
    sessions: List<WorkoutSession>,
    routineId: Long?,
    today: java.time.LocalDate,
    nowMillis: Long,
): WorkoutSession? =
    sessions
        .filter { it.routineId == routineId && it.date == today && it.endEpochMillis != null }
        .filter { nowMillis - it.endEpochMillis!! in 0..SESSION_CONTINUE_WINDOW_MS }
        .maxByOrNull { it.endEpochMillis!! }

/** Groups of finished sessions that are really one workout: same day, same routine, each starting within
 *  [SESSION_CONTINUE_WINDOW_MS] of the previous ending. Freeform sessions (no routine) are never merged. */
fun duplicateSessionGroups(sessions: List<WorkoutSession>): List<List<WorkoutSession>> =
    sessions
        .filter { it.routineId != null && it.endEpochMillis != null }
        .groupBy { it.date to it.routineId }
        .values
        .flatMap { sameDay ->
            val chains = mutableListOf<MutableList<WorkoutSession>>()
            for (session in sameDay.sortedBy { it.startEpochMillis }) {
                val chain = chains.lastOrNull()
                if (chain != null && session.startEpochMillis - chain.last().endEpochMillis!! <= SESSION_CONTINUE_WINDOW_MS) {
                    chain += session
                } else {
                    chains += mutableListOf(session)
                }
            }
            chains.filter { it.size > 1 }
        }
