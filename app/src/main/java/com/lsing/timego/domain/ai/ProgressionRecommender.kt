package com.lsing.timego.domain.ai

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.domain.SetPerformance

data class ProgressionRecommendation(
    val recommendedCatalogueKey: String,
    val rationale: String,
    val isUpgrade: Boolean,
    val pattern: MovementPattern,
    val targetTier: Int
)

class ProgressionRecommender(
    private val preferenceLearner: CategoryPreferenceLearner = CategoryPreferenceLearner()
) {
    /**
     * Evaluates whether an exercise should progress to an advanced tier or alternate modality
     * based on mastery signals and user category lean.
     */
    fun evaluateProgression(
        exercise: Exercise,
        sessionHistory: List<SetPerformance>
    ): ProgressionRecommendation? {
        if (sessionHistory.size < 2) return null

        val profile = BiomechanicalRegistry.getProfile(exercise.catalogueKey)
            ?: CustomExerciseClassifier.classify(exercise.name, exercise.category, exercise.muscleWeights)

        val lastPerformance = sessionHistory.last()
        val isMastered = lastPerformance.reps >= profile.masteryRepCeiling &&
                (lastPerformance.rpe == null || lastPerformance.rpe <= 8)

        if (!isMastered) return null

        val dominantLean = preferenceLearner.getDominantLean()
        val isCalisthenicsLean = dominantLean == ExerciseCategory.CALISTHENICS
        val isStrengthLean = dominantLean == ExerciseCategory.STRENGTH

        // If exercise has a cross-modality equivalent that matches user lean (e.g. user leans calisthenics on a barbell bench)
        if (isCalisthenicsLean && profile.chain == KinematicChain.OKC && profile.crossModalityKey != null) {
            val crossProfile = BiomechanicalRegistry.getProfile(profile.crossModalityKey)
            return ProgressionRecommendation(
                recommendedCatalogueKey = profile.crossModalityKey,
                rationale = "Mastered ${exercise.name}! Based on your Calisthenics preference, try this bodyweight movement.",
                isUpgrade = true,
                pattern = profile.pattern,
                targetTier = crossProfile?.tier ?: (profile.tier + 1)
            )
        }

        if (isStrengthLean && profile.chain == KinematicChain.CKC && profile.crossModalityKey != null) {
            val crossProfile = BiomechanicalRegistry.getProfile(profile.crossModalityKey)
            return ProgressionRecommendation(
                recommendedCatalogueKey = profile.crossModalityKey,
                rationale = "Mastered ${exercise.name}! Based on your Strength preference, try loading with free weights.",
                isUpgrade = true,
                pattern = profile.pattern,
                targetTier = crossProfile?.tier ?: (profile.tier + 1)
            )
        }

        // Direct ladder progression (e.g. Push-up -> Diamond Push-up)
        val nextKey = profile.nextProgressionKey ?: return null
        val nextProfile = BiomechanicalRegistry.getProfile(nextKey)
        val targetTier = nextProfile?.tier ?: (profile.tier + 1)

        return ProgressionRecommendation(
            recommendedCatalogueKey = nextKey,
            rationale = "Mastered ${exercise.name} (${lastPerformance.reps} reps achieved)! Unlocking Tier $targetTier progression.",
            isUpgrade = true,
            pattern = profile.pattern,
            targetTier = targetTier
        )
    }
}
