package com.lsing.timego.profile

import com.lsing.timego.data.TrainingLean

enum class AgeRange {
    UNDER_18,
    AGE_18_24,
    AGE_25_34,
    AGE_35_44,
    AGE_45_54,
    AGE_55_64,
    AGE_65_PLUS,
    PREFER_NOT_TO_SAY,
}

enum class ExperienceLevel { BEGINNER, NOVICE, INTERMEDIATE, ADVANCED }

enum class TrainingGoal { GENERAL_FITNESS, STRENGTH, HYPERTROPHY, ENDURANCE, SKILL }

enum class Equipment {
    BODYWEIGHT,
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    CABLE,
    RESISTANCE_BAND,
    MACHINE,
    PULL_UP_BAR,
    BENCH,
    CARDIO_MACHINE,
    POOL,
}

enum class SessionDurationRange { MINUTES_15_TO_30, MINUTES_30_TO_45, MINUTES_45_TO_60, MINUTES_60_PLUS }

enum class MovementLimitation { SHOULDERS, ELBOWS, WRISTS, BACK, HIPS, KNEES, ANKLES }

data class TrainingProfile(
    val displayName: String? = null,
    val ageRange: AgeRange? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val experience: ExperienceLevel? = null,
    val goal: TrainingGoal? = null,
    val modality: TrainingLean? = null,
    val equipment: Set<Equipment> = emptySet(),
    val trainingDaysPerWeek: Int? = null,
    val sessionDuration: SessionDurationRange? = null,
    val movementLimitations: Set<MovementLimitation> = emptySet(),
    val privateLimitationNote: String? = null,
    val onboardingVersion: Int = 0,
    val invitationDismissed: Boolean = false,
) {
    val hasValidPhysicalValues: Boolean
        get() = heightCm?.let { it in 100.0..250.0 } != false &&
            weightKg?.let { it in 25.0..400.0 } != false

    val isComplete: Boolean
        get() = experience != null &&
            goal != null &&
            modality != null &&
            equipment.isNotEmpty() &&
            trainingDaysPerWeek?.let { it in 1..7 } == true &&
            sessionDuration != null &&
            hasValidPhysicalValues

    fun syncSafeCopy(): TrainingProfile = copy(privateLimitationNote = null)
}

fun inchesToCentimetres(inches: Double): Double = inches * 2.54

fun poundsToKilograms(pounds: Double): Double = pounds * 0.45359237
