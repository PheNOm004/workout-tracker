package com.lsing.timego.domain

/** Rough MET (metabolic equivalent) constants per exercise category, used only for a ballpark
 *  calorie estimate -- not medical-grade, no per-exercise granularity (e.g. running pace bands
 *  aren't distinguished). */
const val MET_CARDIO = 8.0
const val MET_WARMUP = 3.0

/** Standard calorie-burn approximation: MET * body weight (kg) * duration (hours). */
fun estimatedCalorieBurn(met: Double, bodyWeightKg: Double, durationMinutes: Double): Double =
    met * bodyWeightKg * (durationMinutes / 60.0)

/** Null when there's no distance to divide by -- can't compute a pace. */
fun averagePaceMinPerKm(durationMinutes: Double, distanceKm: Double): Double? =
    if (distanceKm > 0.0) durationMinutes / distanceKm else null
