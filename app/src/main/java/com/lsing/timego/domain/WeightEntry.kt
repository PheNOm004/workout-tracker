package com.lsing.timego.domain

/** Dumbbell-style exercises can be typed per hand or as both hands combined. Stored weights are always
 *  per hand, so history, suggestions and charts stay comparable whichever way an exercise is entered. */
fun combinedEntryToStored(entered: Double, combined: Boolean): Double = if (combined) entered / 2.0 else entered

fun storedToCombinedEntry(stored: Double, combined: Boolean): Double = if (combined) stored * 2.0 else stored

private val DUMBBELL_STYLE = Regex("dumbbell|kettlebell|\bdb\b|lateral raise|reverse fly|bulgarian|hammer curl|goblet|shrug|farmer", RegexOption.IGNORE_CASE)
private val NOT_DUMBBELL = Regex("cable|machine|barbell|smith|pulldown|pushdown|press machine", RegexOption.IGNORE_CASE)
private val SIDE_BY_SIDE = Regex("stretch|single|one-arm|one-leg|unilateral|side |pigeon|copenhagen|archer|pistol|lunge|hip flexor|quad|hamstring|calf|ankle|wrist", RegexOption.IGNORE_CASE)

/** Whether the "both dumbbells combined" choice is relevant; keeps the checkbox off hack squats, machines etc. */
fun supportsCombinedEntry(exerciseName: String): Boolean =
    DUMBBELL_STYLE.containsMatchIn(exerciseName) && !NOT_DUMBBELL.containsMatchIn(exerciseName)

/** Whether a timed hold is usually done one side at a time (stretches, single-limb holds). */
fun supportsBothSides(exerciseName: String): Boolean = SIDE_BY_SIDE.containsMatchIn(exerciseName)
