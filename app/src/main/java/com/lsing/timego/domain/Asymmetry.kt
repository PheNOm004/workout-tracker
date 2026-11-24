package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import kotlin.math.abs

object Side {
    const val LEFT = "LEFT"
    const val RIGHT = "RIGHT"
    fun other(side: String): String = if (side == LEFT) RIGHT else LEFT
    fun label(side: String): String = if (side == LEFT) "Left" else "Right"
}

/** [pending] is the side logged first in a pair still waiting for its partner; [startRest] says whether this set ends the pair. */
data class SidePairStep(val pending: String?, val startRest: Boolean)

/** Rest waits for the second side of a pair. Logging the same side twice in a row is an escape hatch: rest starts at once. */
fun stepSidePair(pending: String?, loggedSide: String): SidePairStep = when (pending) {
    null -> SidePairStep(loggedSide, startRest = false)
    else -> SidePairStep(null, startRest = true)
}

/** Bounded left/right difference, positive when the right side is stronger; null when neither side has a value. */
fun asymmetryPercent(left: Double, right: Double): Double? {
    val stronger = maxOf(left, right)
    return if (stronger <= 0.0) null else (right - left) / stronger * 100.0
}

/** Per-session asymmetry: the k-th working left set is paired with the k-th working right set; orphans are ignored.
 *  Compared on estimated 1RM, so a 10 versus 8 rep gap at equal load reads as the roughly 5% strength gap it is. */
fun pairedSessionAsymmetries(sets: List<SetLog>): List<Pair<Long, Double>> =
    sets.filter { !it.isWarmup && it.side != null && it.weightKg > 0.0 }
        .groupBy { it.sessionId }
        .mapNotNull { (sessionId, sessionSets) ->
            val ordered = sessionSets.sortedBy { it.loggedAtEpochMillis }
            val lefts = ordered.filter { it.side == Side.LEFT }
            val rights = ordered.filter { it.side == Side.RIGHT }
            val percents = lefts.zip(rights).mapNotNull { (l, r) ->
                asymmetryPercent(estimatedOneRepMax(l.weightKg, l.reps), estimatedOneRepMax(r.weightKg, r.reps))
            }
            if (percents.isEmpty()) null else sessionId to percents.average()
        }

enum class AsymmetryStatus { WATCH, DEFICIT }

data class AsymmetryInsight(val status: AsymmetryStatus, val laggingSide: String, val percent: Double, val sessions: Int) {
    val message: String get() = "${Side.label(laggingSide)} lags by ${"%.0f".format(percent)}% over $sessions sessions"
}

private const val MIN_SESSIONS = 3
private const val WATCH_PERCENT = 10.0
private const val DEFICIT_PERCENT = 15.0

/** A flag only after at least [MIN_SESSIONS] recent sessions agree on a gap of 10% or more, so one odd day never alarms.
 *  [sets] are one exercise's sets from the window the caller cares about (the last 60 days is typical). */
fun asymmetryInsight(sets: List<SetLog>): AsymmetryInsight? {
    val perSession = pairedSessionAsymmetries(sets)
    if (perSession.size < MIN_SESSIONS) return null
    val mean = perSession.map { it.second }.average()
    val direction = if (mean >= 0) 1 else -1
    val agreeing = perSession.count { abs(it.second) >= WATCH_PERCENT && (it.second >= 0) == (direction == 1) }
    if (agreeing < MIN_SESSIONS || abs(mean) < WATCH_PERCENT) return null
    return AsymmetryInsight(
        status = if (abs(mean) > DEFICIT_PERCENT) AsymmetryStatus.DEFICIT else AsymmetryStatus.WATCH,
        laggingSide = if (direction == 1) Side.LEFT else Side.RIGHT,
        percent = abs(mean),
        sessions = agreeing,
    )
}
