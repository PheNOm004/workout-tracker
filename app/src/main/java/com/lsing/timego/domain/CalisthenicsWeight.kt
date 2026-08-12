package com.lsing.timego.domain

/** Formats a calisthenics set's added weight as "BW + k" instead of a raw absolute kg number --
 *  [addedWeightKg] is display-only (see [SetLog.addedWeightKg]); the underlying domain math still
 *  uses the full bodyweight+k total. Negative values (shouldn't occur, but not worth crashing over)
 *  display the same as zero rather than showing "BW + -2.5kg". */
fun formatCalisthenicsWeight(addedWeightKg: Double): String =
    if (addedWeightKg <= 0.0) "BW" else "BW + %.1fkg".format(addedWeightKg)
