package com.lsing.timego.domain.programs

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.ai.ProgressionRecommender
import com.lsing.timego.domain.suggestedExerciseFor

/** Resolves one [ProgramSlot] to a specific [Exercise], given the current [exercises] catalogue,
 *  [lean] preference, [usageCounts] and [recentLogsByExerciseId] (recent history per exercise, used
 *  only for movementPattern-tagged slots). Default slots (movementPattern == null) always go
 *  through [suggestedExerciseFor], identical to the existing landing-page single-exercise path --
 *  this is a regression-safety requirement, not an implementation shortcut.
 *
 *  movementPattern-tagged slots (Advanced calisthenics only) attempt a ladder-progression lookup via
 *  [ProgressionRecommender] against whichever matching exercise has the most recent logged history;
 *  if that recommender has no history to evaluate, or the recommended catalogueKey isn't present in
 *  [exercises] (this app's catalogue is the source of truth, not the recommender's key space), this
 *  falls back to the same [suggestedExerciseFor] path as a default slot. */
fun resolveSlot(
    slot: ProgramSlot,
    exercises: List<Exercise>,
    lean: TrainingLean,
    usageCounts: Map<Long, Int>,
    recentLogsByExerciseId: Map<Long, List<SetPerformance>>,
    progressionRecommender: ProgressionRecommender = ProgressionRecommender(),
): Exercise? {
    val fallback = { suggestedExerciseFor(slot.targetGroups, exercises, lean, usageCounts) }
    if (slot.movementPattern == null) return fallback()

    val candidatesWithHistory = exercises.filter { exercise ->
        exercise.muscleGroups.any { it in slot.targetGroups } && recentLogsByExerciseId[exercise.id]?.isNotEmpty() == true
    }
    val mostRecentExercise = candidatesWithHistory.maxByOrNull { recentLogsByExerciseId[it.id]?.size ?: 0 }
        ?: return fallback()

    val history = recentLogsByExerciseId[mostRecentExercise.id].orEmpty()
    val recommendation = progressionRecommender.evaluateProgression(mostRecentExercise, history) ?: return fallback()

    return exercises.firstOrNull { it.catalogueKey == recommendation.recommendedCatalogueKey } ?: fallback()
}
