package com.lsing.timego.domain.ai

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog

data class BottleneckDiagnosis(
    val exerciseName: String,
    val primeMover: String,
    val weakLinkMuscle: String,
    val strengthAccessories: List<Exercise>,
    val calisthenicsAccessories: List<Exercise>,
    val diagnosticReason: String
)

/**
 * Universal Weak-Link & Accessory Engine.
 * Operates across all 820 exercises in TimeGo to identify mechanical choke points,
 * lagging synergist muscles, and cross-modality accessory bridges.
 */
class WeakLinkDiagnostician {

    /**
     * Analyzes an exercise that is plateauing or struggling to diagnose its mechanical bottleneck
     * and recommend targeted strength and calisthenics accessories.
     *
     * @param exercise The exercise currently stalling.
     * @param allExercises The full library of available exercises (820+ seeds + custom).
     * @param recentSets The user's recent set logs (e.g. past 14-30 days) to compute synergist training volume.
     */
    fun diagnoseBottleneck(
        exercise: Exercise,
        allExercises: List<Exercise>,
        recentSets: List<SetLog>
    ): BottleneckDiagnosis? {
        val weights = exercise.muscleWeights
        if (weights.isEmpty() && exercise.muscleGroups.size <= 1) return null

        // 1. Determine Prime Mover and Limiting Synergists
        val (primeMover, synergists) = if (weights.isNotEmpty()) {
            val prime = weights.maxByOrNull { it.value }?.key ?: exercise.muscleGroups.first()
            val syns = weights.filter { (muscle, weight) ->
                muscle != prime && weight in 25..75
            }.keys.toList()
            prime to syns
        } else {
            val prime = exercise.muscleGroups.first()
            val syns = exercise.muscleGroups.drop(1)
            prime to syns
        }

        if (synergists.isEmpty()) return null

        // 2. Tally recent set volume for each synergist muscle group from user logs
        val exerciseById = allExercises.associateBy { it.id }
        val synergistVolume = synergists.associateWith { muscle ->
            recentSets.count { set ->
                val ex = exerciseById[set.exerciseId]
                ex != null && ex.muscleGroups.contains(muscle) && !set.isWarmup
            }
        }

        // The weak link is the synergist with the lowest training volume in recent logs
        val weakLink = synergistVolume.minByOrNull { it.value }?.key ?: synergists.first()

        // 3. Find top accessory movements for this weak link across Strength and Calisthenics
        val eligibleAccessories = allExercises.filter { candidate ->
            candidate.id != exercise.id &&
            candidate.muscleGroups.contains(weakLink) &&
            // Prefer exercises where the weak link is the prime mover (weight >= 70)
            (candidate.muscleWeights[weakLink] ?: 100) >= 60
        }

        val strengthAcc = eligibleAccessories
            .filter { it.category == ExerciseCategory.STRENGTH.name }
            .take(3)

        val calisthenicsAcc = eligibleAccessories
            .filter { it.category == ExerciseCategory.CALISTHENICS.name }
            .take(3)

        val weakLinkClean = weakLink.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val primeClean = primeMover.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

        val reason = "Bottleneck detected: $weakLinkClean support limits $primeClean force output."

        return BottleneckDiagnosis(
            exerciseName = exercise.name,
            primeMover = primeClean,
            weakLinkMuscle = weakLinkClean,
            strengthAccessories = strengthAcc,
            calisthenicsAccessories = calisthenicsAcc,
            diagnosticReason = reason
        )
    }
}
