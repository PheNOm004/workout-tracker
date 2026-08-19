package com.lsing.timego.domain

import com.lsing.timego.data.BodyMetric

/** Most recent non-null weight/height across [metrics]. Both the Log screen (calisthenics
 *  bodyweight) and the Progress screen (BMI) need this, and both previously open-coded a
 *  `lastOrNull { ... }` that silently depended on BodyMetricDao.observeAll's `ORDER BY date`.
 *  Sorting here makes that dependency explicit instead of ambient. */
fun latestWeightKg(metrics: List<BodyMetric>): Double? =
    metrics.sortedBy { it.date }.lastOrNull { it.weightKg != null }?.weightKg

fun latestHeightCm(metrics: List<BodyMetric>): Double? =
    metrics.sortedBy { it.date }.lastOrNull { it.heightCm != null }?.heightCm

/** Standard BMI formula: weight (kg) / height (m) squared. */
fun bodyMassIndex(weightKg: Double, heightCm: Double): Double {
    val heightM = heightCm / 100.0
    return weightKg / (heightM * heightM)
}

enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

/** Standard WHO adult BMI bands -- doesn't account for age, sex, or body composition, same
 *  caveats as any BMI-based estimate. */
fun bmiCategory(bmi: Double): BmiCategory = when {
    bmi < 18.5 -> BmiCategory.UNDERWEIGHT
    bmi < 25.0 -> BmiCategory.NORMAL
    bmi < 30.0 -> BmiCategory.OVERWEIGHT
    else -> BmiCategory.OBESE
}
