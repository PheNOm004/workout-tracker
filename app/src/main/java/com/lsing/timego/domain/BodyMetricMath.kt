package com.lsing.timego.domain

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
