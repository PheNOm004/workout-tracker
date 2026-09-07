package com.lsing.timego.domain.ai

import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup

object CustomExerciseClassifier {

    /**
     * Infers a BiomechanicalProfile for an uncatalogued or custom user exercise
     * using name semantics and muscle weight distributions.
     */
    fun classify(
        name: String,
        category: String,
        muscleWeights: Map<String, Int>
    ): BiomechanicalProfile {
        val lowerName = name.lowercase()
        val isCalisthenics = category == ExerciseCategory.CALISTHENICS.name
        val chain = if (isCalisthenics) KinematicChain.CKC else KinematicChain.OKC

        // 1. Check Cardio first
        if (category == ExerciseCategory.CARDIO.name ||
            lowerName.contains("run") || lowerName.contains("jog") ||
            lowerName.contains("bike") || lowerName.contains("cycl") ||
            lowerName.contains("row") && muscleWeights.containsKey(MuscleGroup.FULL_BODY.name)
        ) {
            val isInterval = lowerName.contains("sprint") || lowerName.contains("hiit") || lowerName.contains("jump rope")
            return BiomechanicalProfile(
                pattern = if (isInterval) MovementPattern.CARDIO_INTERVAL else MovementPattern.CARDIO_STEADY,
                chain = KinematicChain.CKC,
                tier = 3,
                masteryRepCeiling = 30
            )
        }

        // 2. Check Core & Rotational
        val coreWeight = (muscleWeights[MuscleGroup.ABS.name] ?: 0) + (muscleWeights[MuscleGroup.OBLIQUES.name] ?: 0)
        if (coreWeight >= 60 || lowerName.contains("plank") || lowerName.contains("flag") || lowerName.contains("twist")) {
            return BiomechanicalProfile(
                pattern = MovementPattern.CORE_ROTATIONAL,
                chain = chain,
                tier = 3,
                masteryRepCeiling = 15
            )
        }

        // 3. Check Legs
        val quadWeight = muscleWeights[MuscleGroup.QUADS.name] ?: 0
        val hamWeight = muscleWeights[MuscleGroup.HAMSTRINGS.name] ?: 0
        val gluteWeight = muscleWeights[MuscleGroup.GLUTES.name] ?: 0

        if (quadWeight > 0 || hamWeight > 0 || gluteWeight > 0 || lowerName.contains("squat") || lowerName.contains("deadlift") || lowerName.contains("lunge")) {
            val isHinge = hamWeight > quadWeight || lowerName.contains("deadlift") || lowerName.contains("rdl") || lowerName.contains("hinge") || lowerName.contains("nordic")
            return BiomechanicalProfile(
                pattern = if (isHinge) MovementPattern.HIP_DOMINANT else MovementPattern.KNEE_DOMINANT,
                chain = chain,
                tier = 3,
                masteryRepCeiling = if (isHinge) 10 else 12
            )
        }

        // 4. Upper Body Pull
        val latWeight = muscleWeights[MuscleGroup.LATS.name] ?: 0
        val backWeight = (muscleWeights[MuscleGroup.UPPER_BACK.name] ?: 0) + (muscleWeights[MuscleGroup.TRAPS.name] ?: 0)
        if (latWeight > 0 || backWeight > 0 || lowerName.contains("pull") || lowerName.contains("row") || lowerName.contains("chin")) {
            val isVertical = lowerName.contains("pullup") || lowerName.contains("chin") || lowerName.contains("pulldown") || (latWeight > backWeight * 1.3)
            return BiomechanicalProfile(
                pattern = if (isVertical) MovementPattern.VERTICAL_PULL else MovementPattern.HORIZONTAL_PULL,
                chain = chain,
                tier = 3,
                masteryRepCeiling = if (isVertical) 10 else 12
            )
        }

        // 5. Upper Body Push (Default upper body)
        val chestWeight = muscleWeights[MuscleGroup.CHEST.name] ?: 0
        val deltWeight = (muscleWeights[MuscleGroup.FRONT_DELTS.name] ?: 0) + (muscleWeights[MuscleGroup.SIDE_DELTS.name] ?: 0)
        val isVerticalPush = lowerName.contains("overhead") || lowerName.contains("shoulder") || lowerName.contains("pike") || lowerName.contains("handstand") || (deltWeight > chestWeight * 1.2)

        return BiomechanicalProfile(
            pattern = if (isVerticalPush) MovementPattern.VERTICAL_PUSH else MovementPattern.HORIZONTAL_PUSH,
            chain = chain,
            tier = 3,
            masteryRepCeiling = 12
        )
    }
}
