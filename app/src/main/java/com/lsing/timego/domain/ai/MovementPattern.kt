package com.lsing.timego.domain.ai

enum class MovementPattern {
    HORIZONTAL_PUSH,
    VERTICAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PULL,
    KNEE_DOMINANT,
    HIP_DOMINANT,
    CORE_ROTATIONAL,
    CARDIO_STEADY,
    CARDIO_INTERVAL;

    val isCardio: Boolean
        get() = this == CARDIO_STEADY || this == CARDIO_INTERVAL

    val isUpperBody: Boolean
        get() = this == HORIZONTAL_PUSH || this == VERTICAL_PUSH ||
                this == HORIZONTAL_PULL || this == VERTICAL_PULL

    val isLowerBody: Boolean
        get() = this == KNEE_DOMINANT || this == HIP_DOMINANT
}

enum class KinematicChain {
    /** Closed Kinetic Chain: body moves through space against a fixed object (typical of Calisthenics: Push-up, Pull-up, Dip, Squat). */
    CKC,

    /** Open Kinetic Chain: distal extremity moves a load while body is stationary (typical of Barbell/Dumbbell/Machine: Bench Press, Lat Pulldown). */
    OKC
}

/**
 * Biomechanical profile defining where an exercise sits in human movement physics,
 * its difficulty tier, and its adjacent progression edges.
 */
data class BiomechanicalProfile(
    val pattern: MovementPattern,
    val chain: KinematicChain,
    val tier: Int, // 1 (Beginner) to 10 (Elite)
    val masteryRepCeiling: Int = 15, // Reps threshold at moderate RPE to signal mastery
    val nextProgressionKey: String? = null,
    val regressionKey: String? = null,
    val crossModalityKey: String? = null
)
