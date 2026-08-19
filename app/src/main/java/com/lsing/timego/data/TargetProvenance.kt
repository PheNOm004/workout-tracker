package com.lsing.timego.data

/**
 * Distinguishes a genuine target that was visible before a set from a freeform value filled after
 * it. This is evidence metadata only; it does not change the existing overload suggestion logic.
 */
enum class TargetProvenance {
    UNKNOWN,
    OVERLOAD_SUGGESTION,
}

fun targetProvenanceFor(suggestionWasShown: Boolean): TargetProvenance =
    if (suggestionWasShown) TargetProvenance.OVERLOAD_SUGGESTION else TargetProvenance.UNKNOWN
