package com.lsing.timego.domain.ai

import java.util.Locale

internal fun seedKey(name: String): String =
    "timego.seed.v1." + name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')

object BiomechanicalRegistry {

    private val registry: MutableMap<String, BiomechanicalProfile> = mutableMapOf()

    init {
        // ==========================================
        // 1. HORIZONTAL PUSH
        // ==========================================
        // Calisthenics ladder
        register(
            "Wall Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 1,
                masteryRepCeiling = 20,
                nextProgressionKey = seedKey("Knee Push-Up"),
                crossModalityKey = seedKey("Dumbbell Bench Press")
            )
        )
        register(
            "Knee Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 2,
                masteryRepCeiling = 20,
                nextProgressionKey = seedKey("Push-Up"),
                regressionKey = seedKey("Wall Push-Up"),
                crossModalityKey = seedKey("Dumbbell Bench Press")
            )
        )
        register(
            "Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 3,
                masteryRepCeiling = 20,
                nextProgressionKey = seedKey("Decline Push-Up"),
                regressionKey = seedKey("Knee Push-Up"),
                crossModalityKey = seedKey("Barbell Bench Press")
            )
        )
        register(
            "Decline Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Diamond Push-Up"),
                regressionKey = seedKey("Push-Up"),
                crossModalityKey = seedKey("Incline Barbell Bench Press")
            )
        )
        register(
            "Diamond Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 5,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Archer Push-Up"),
                regressionKey = seedKey("Decline Push-Up"),
                crossModalityKey = seedKey("Close-Grip Bench Press")
            )
        )
        register(
            "Archer Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 6,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Pseudo Planche Push-Up"),
                regressionKey = seedKey("Diamond Push-Up"),
                crossModalityKey = seedKey("Barbell Bench Press")
            )
        )
        register(
            "Pseudo Planche Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 7,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Planche Push-Up"),
                regressionKey = seedKey("Archer Push-Up")
            )
        )
        register(
            "Planche Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 9,
                masteryRepCeiling = 8,
                regressionKey = seedKey("Pseudo Planche Push-Up")
            )
        )

        register(
            "Dip",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Ring Dip"),
                regressionKey = seedKey("Diamond Push-Up"),
                crossModalityKey = seedKey("Weighted Chest Dip")
            )
        )
        register(
            "Ring Dip",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 6,
                masteryRepCeiling = 12,
                regressionKey = seedKey("Dip"),
                crossModalityKey = seedKey("Weighted Chest Dip")
            )
        )
        register(
            "Weighted Chest Dip",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.OKC,
                tier = 5,
                masteryRepCeiling = 10,
                regressionKey = seedKey("Dip"),
                crossModalityKey = seedKey("Ring Dip")
            )
        )

        // Strength track
        register(
            "Dumbbell Bench Press",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.OKC,
                tier = 3,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Barbell Bench Press"),
                crossModalityKey = seedKey("Push-Up")
            )
        )
        register(
            "Barbell Bench Press",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.OKC,
                tier = 4,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Incline Barbell Bench Press"),
                regressionKey = seedKey("Dumbbell Bench Press"),
                crossModalityKey = seedKey("Diamond Push-Up")
            )
        )
        register(
            "Incline Barbell Bench Press",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PUSH,
                chain = KinematicChain.OKC,
                tier = 5,
                masteryRepCeiling = 10,
                regressionKey = seedKey("Barbell Bench Press"),
                crossModalityKey = seedKey("Decline Push-Up")
            )
        )

        // ==========================================
        // 2. VERTICAL PUSH
        // ==========================================
        register(
            "Pike Walk",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 2,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Wall Handstand Hold"),
                crossModalityKey = seedKey("Seated Dumbbell Shoulder Press")
            )
        )
        register(
            "Wall Handstand Hold",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 60, // seconds
                nextProgressionKey = seedKey("Wall-Assisted Handstand Push-Up Negative"),
                regressionKey = seedKey("Pike Walk"),
                crossModalityKey = seedKey("Overhead Press")
            )
        )
        register(
            "Wall-Assisted Handstand Push-Up Negative",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 5,
                masteryRepCeiling = 8,
                nextProgressionKey = seedKey("Handstand Push-Up"),
                regressionKey = seedKey("Wall Handstand Hold")
            )
        )
        register(
            "Handstand Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 7,
                masteryRepCeiling = 8,
                nextProgressionKey = seedKey("Deficit Handstand Push-Up"),
                regressionKey = seedKey("Wall-Assisted Handstand Push-Up Negative"),
                crossModalityKey = seedKey("Overhead Press")
            )
        )
        register(
            "Deficit Handstand Push-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.CKC,
                tier = 8,
                masteryRepCeiling = 6,
                regressionKey = seedKey("Handstand Push-Up")
            )
        )

        // Strength vertical push
        register(
            "Seated Dumbbell Shoulder Press",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.OKC,
                tier = 3,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Overhead Press"),
                crossModalityKey = seedKey("Pike Walk")
            )
        )
        register(
            "Overhead Press",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PUSH,
                chain = KinematicChain.OKC,
                tier = 4,
                masteryRepCeiling = 10,
                regressionKey = seedKey("Seated Dumbbell Shoulder Press"),
                crossModalityKey = seedKey("Handstand Push-Up")
            )
        )

        // ==========================================
        // 3. HORIZONTAL PULL
        // ==========================================
        register(
            "Australian Pull-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.CKC,
                tier = 2,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Barbell Inverted Row"),
                crossModalityKey = seedKey("Single-Arm Dumbbell Row")
            )
        )
        register(
            "Barbell Inverted Row",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.CKC,
                tier = 3,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Tuck Front Lever"),
                regressionKey = seedKey("Australian Pull-Up"),
                crossModalityKey = seedKey("Barbell Row")
            )
        )
        register(
            "Tuck Front Lever",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.CKC,
                tier = 5,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Advanced Tuck Front Lever"),
                regressionKey = seedKey("Barbell Inverted Row"),
                crossModalityKey = seedKey("Barbell Row")
            )
        )
        register(
            "Advanced Tuck Front Lever",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.CKC,
                tier = 6,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Straddle Front Lever"),
                regressionKey = seedKey("Tuck Front Lever"),
                crossModalityKey = seedKey("Pendlay Row")
            )
        )
        register(
            "Straddle Front Lever",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.CKC,
                tier = 7,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Front Lever Hold"),
                regressionKey = seedKey("Advanced Tuck Front Lever")
            )
        )
        register(
            "Front Lever Hold",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.CKC,
                tier = 8,
                masteryRepCeiling = 10,
                regressionKey = seedKey("Straddle Front Lever")
            )
        )
        register(
            "Single-Arm Dumbbell Row",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.OKC,
                tier = 3,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Barbell Row"),
                crossModalityKey = seedKey("Australian Pull-Up")
            )
        )
        register(
            "Barbell Row",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.OKC,
                tier = 4,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Pendlay Row"),
                regressionKey = seedKey("Single-Arm Dumbbell Row"),
                crossModalityKey = seedKey("Barbell Inverted Row")
            )
        )
        register(
            "Pendlay Row",
            BiomechanicalProfile(
                pattern = MovementPattern.HORIZONTAL_PULL,
                chain = KinematicChain.OKC,
                tier = 5,
                masteryRepCeiling = 8,
                regressionKey = seedKey("Barbell Row")
            )
        )

        // ==========================================
        // 4. VERTICAL PULL
        // ==========================================
        register(
            "Dead Hang",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PULL,
                chain = KinematicChain.CKC,
                tier = 1,
                masteryRepCeiling = 60, // seconds
                nextProgressionKey = seedKey("Pull-Up"),
                crossModalityKey = seedKey("Lat Pulldown")
            )
        )
        register(
            "Lat Pulldown",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PULL,
                chain = KinematicChain.OKC,
                tier = 2,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Pull-Up"),
                crossModalityKey = seedKey("Dead Hang")
            )
        )
        register(
            "Pull-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PULL,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Archer Pull-Up"),
                regressionKey = seedKey("Dead Hang"),
                crossModalityKey = seedKey("Lat Pulldown")
            )
        )
        register(
            "Archer Pull-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PULL,
                chain = KinematicChain.CKC,
                tier = 6,
                masteryRepCeiling = 8,
                nextProgressionKey = seedKey("Muscle-Up"),
                regressionKey = seedKey("Pull-Up")
            )
        )
        register(
            "Muscle-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.VERTICAL_PULL,
                chain = KinematicChain.CKC,
                tier = 8,
                masteryRepCeiling = 6,
                regressionKey = seedKey("Archer Pull-Up")
            )
        )

        // ==========================================
        // 5. KNEE-DOMINANT (SQUAT)
        // ==========================================
        register(
            "Wall Sit",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 1,
                masteryRepCeiling = 60, // seconds
                nextProgressionKey = seedKey("Step-Up")
            )
        )
        register(
            "Step-Up",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 2,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Bulgarian Split Squat"),
                regressionKey = seedKey("Wall Sit"),
                crossModalityKey = seedKey("Goblet Squat")
            )
        )
        register(
            "Bulgarian Split Squat",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Assisted Pistol Squat"),
                regressionKey = seedKey("Step-Up"),
                crossModalityKey = seedKey("Barbell Back Squat")
            )
        )
        register(
            "Assisted Pistol Squat",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 5,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Pistol Squat"),
                regressionKey = seedKey("Bulgarian Split Squat")
            )
        )
        register(
            "Pistol Squat",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 7,
                masteryRepCeiling = 8,
                regressionKey = seedKey("Assisted Pistol Squat"),
                crossModalityKey = seedKey("Barbell Front Squat")
            )
        )

        // Strength squat track
        register(
            "Barbell Back Squat",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.OKC,
                tier = 4,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Barbell Front Squat"),
                crossModalityKey = seedKey("Bulgarian Split Squat")
            )
        )
        register(
            "Barbell Front Squat",
            BiomechanicalProfile(
                pattern = MovementPattern.KNEE_DOMINANT,
                chain = KinematicChain.OKC,
                tier = 6,
                masteryRepCeiling = 8,
                regressionKey = seedKey("Barbell Back Squat"),
                crossModalityKey = seedKey("Pistol Squat")
            )
        )

        // ==========================================
        // 6. HIP-DOMINANT (HINGE)
        // ==========================================
        register(
            "Single-Leg Glute Bridge",
            BiomechanicalProfile(
                pattern = MovementPattern.HIP_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 2,
                masteryRepCeiling = 15,
                nextProgressionKey = seedKey("Nordic Curl Negative"),
                crossModalityKey = seedKey("Romanian Deadlift")
            )
        )
        register(
            "Nordic Curl Negative",
            BiomechanicalProfile(
                pattern = MovementPattern.HIP_DOMINANT,
                chain = KinematicChain.CKC,
                tier = 6,
                masteryRepCeiling = 8,
                regressionKey = seedKey("Single-Leg Glute Bridge"),
                crossModalityKey = seedKey("Conventional Deadlift")
            )
        )
        register(
            "Romanian Deadlift",
            BiomechanicalProfile(
                pattern = MovementPattern.HIP_DOMINANT,
                chain = KinematicChain.OKC,
                tier = 3,
                masteryRepCeiling = 10,
                nextProgressionKey = seedKey("Conventional Deadlift"),
                crossModalityKey = seedKey("Single-Leg Glute Bridge")
            )
        )
        register(
            "Conventional Deadlift",
            BiomechanicalProfile(
                pattern = MovementPattern.HIP_DOMINANT,
                chain = KinematicChain.OKC,
                tier = 5,
                masteryRepCeiling = 8,
                nextProgressionKey = seedKey("Deficit Deadlift"),
                regressionKey = seedKey("Romanian Deadlift"),
                crossModalityKey = seedKey("Nordic Curl Negative")
            )
        )
        register(
            "Deficit Deadlift",
            BiomechanicalProfile(
                pattern = MovementPattern.HIP_DOMINANT,
                chain = KinematicChain.OKC,
                tier = 6,
                masteryRepCeiling = 6,
                regressionKey = seedKey("Conventional Deadlift")
            )
        )

        // ==========================================
        // 7. CORE & ROTATIONAL
        // ==========================================
        register(
            "L-Sit",
            BiomechanicalProfile(
                pattern = MovementPattern.CORE_ROTATIONAL,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 30, // seconds
                nextProgressionKey = seedKey("Toes to Bar"),
                crossModalityKey = seedKey("Cable Woodchopper Down")
            )
        )
        register(
            "Toes to Bar",
            BiomechanicalProfile(
                pattern = MovementPattern.CORE_ROTATIONAL,
                chain = KinematicChain.CKC,
                tier = 5,
                masteryRepCeiling = 12,
                nextProgressionKey = seedKey("Dragon Flag"),
                regressionKey = seedKey("L-Sit")
            )
        )
        register(
            "Dragon Flag",
            BiomechanicalProfile(
                pattern = MovementPattern.CORE_ROTATIONAL,
                chain = KinematicChain.CKC,
                tier = 7,
                masteryRepCeiling = 8,
                regressionKey = seedKey("Toes to Bar")
            )
        )

        // ==========================================
        // 8. CARDIO (STEADY & INTERVAL)
        // ==========================================
        register(
            "Incline Walking",
            BiomechanicalProfile(
                pattern = MovementPattern.CARDIO_STEADY,
                chain = KinematicChain.CKC,
                tier = 1,
                masteryRepCeiling = 30, // minutes
                nextProgressionKey = seedKey("Stationary Bike")
            )
        )
        register(
            "Stationary Bike",
            BiomechanicalProfile(
                pattern = MovementPattern.CARDIO_STEADY,
                chain = KinematicChain.CKC,
                tier = 2,
                masteryRepCeiling = 45, // minutes
                nextProgressionKey = seedKey("Treadmill Running"),
                regressionKey = seedKey("Incline Walking")
            )
        )
        register(
            "Treadmill Running",
            BiomechanicalProfile(
                pattern = MovementPattern.CARDIO_STEADY,
                chain = KinematicChain.CKC,
                tier = 3,
                masteryRepCeiling = 30,
                nextProgressionKey = seedKey("Running"),
                regressionKey = seedKey("Stationary Bike")
            )
        )
        register(
            "Running",
            BiomechanicalProfile(
                pattern = MovementPattern.CARDIO_STEADY,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 45,
                nextProgressionKey = seedKey("Rowing Machine"),
                regressionKey = seedKey("Treadmill Running")
            )
        )
        register(
            "Rowing Machine",
            BiomechanicalProfile(
                pattern = MovementPattern.CARDIO_STEADY,
                chain = KinematicChain.CKC,
                tier = 5,
                masteryRepCeiling = 30,
                regressionKey = seedKey("Running")
            )
        )
        register(
            "Jump Rope",
            BiomechanicalProfile(
                pattern = MovementPattern.CARDIO_INTERVAL,
                chain = KinematicChain.CKC,
                tier = 4,
                masteryRepCeiling = 15 // minutes intervals
            )
        )
    }

    private fun register(name: String, profile: BiomechanicalProfile) {
        registry[seedKey(name)] = profile
    }

    fun getProfile(catalogueKey: String?): BiomechanicalProfile? {
        if (catalogueKey == null) return null
        return registry[catalogueKey]
    }

    fun getAll(): Map<String, BiomechanicalProfile> = registry
}

