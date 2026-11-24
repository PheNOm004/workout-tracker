package com.lsing.timego.domain

/** Per-hand weight steps the stepper cycles through. */
val WEIGHT_STEP_CHOICES = listOf(1.0, 1.25, 2.0, 2.5, 5.0)

/** [override] is the user's pick for this exercise. Otherwise dumbbells (named so, or logged as a combined
 *  pair) step 1 kg and everything else the 2.5 kg plate step. */
fun weightStepKg(exerciseName: String, combinedEntry: Boolean, override: Double?): Double =
    override ?: if (combinedEntry || exerciseName.contains("dumbbell", ignoreCase = true)) 1.0 else DEFAULT_WEIGHT_INCREMENT_KG

fun nextWeightStep(current: Double): Double =
    WEIGHT_STEP_CHOICES.firstOrNull { it > current } ?: WEIGHT_STEP_CHOICES.first()
