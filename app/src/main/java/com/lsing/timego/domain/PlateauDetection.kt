package com.lsing.timego.domain

enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING }

private const val WINDOW_SIZE = 5

/** Classifies training trend from a metric history (oldest first) and whether each entry hit its
 *  target -- shared by [RuleBasedOverloadSuggester] (estimated 1RM) and [RuleBasedHoldSuggester]
 *  (hold duration), which differ only in which metric they extract. REGRESSING (last two entries
 *  both missed target) is checked first regardless of history length -- this is the same trigger
 *  as the pre-upgrade suggesters' deload rule, now labeled. With fewer than [WINDOW_SIZE] entries
 *  there isn't enough history for a trend, so anything short of REGRESSING falls back to
 *  PROGRESSING (today's original behavior) rather than ever claiming PLATEAUING. With
 *  [WINDOW_SIZE]+ entries, only the most recent [WINDOW_SIZE] are considered (older entries are
 *  windowed out, not averaged in) -- PROGRESSING if the latest value is at or above the average of
 *  the preceding entries in the window, or if the window's second half averages strictly higher
 *  than its first half (the middle entry, if any, belongs to neither half -- this is a stricter
 *  trend check than comparing endpoints, since an oscillating window that returns to its starting
 *  value would otherwise misread as "trending up"); PLATEAUING otherwise. */
fun classifyPlateauStatus(recentValues: List<Double>, hitTargetFlags: List<Boolean>): PlateauStatus {
    require(recentValues.size == hitTargetFlags.size) { "recentValues and hitTargetFlags must be the same size" }

    val lastTwoMissed = hitTargetFlags.size >= 2 &&
        !hitTargetFlags[hitTargetFlags.size - 1] &&
        !hitTargetFlags[hitTargetFlags.size - 2]
    if (lastTwoMissed) return PlateauStatus.REGRESSING

    if (recentValues.size < WINDOW_SIZE) return PlateauStatus.PROGRESSING

    val window = recentValues.takeLast(WINDOW_SIZE)
    val precedingAverage = window.dropLast(1).average()
    val halfSize = window.size / 2
    val firstHalfAverage = window.take(halfSize).average()
    val secondHalfAverage = window.takeLast(halfSize).average()
    val trendingUp = secondHalfAverage > firstHalfAverage
    return if (window.last() >= precedingAverage || trendingUp) PlateauStatus.PROGRESSING else PlateauStatus.PLATEAUING
}
