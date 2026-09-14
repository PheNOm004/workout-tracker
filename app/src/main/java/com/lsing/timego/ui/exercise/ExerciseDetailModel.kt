package com.lsing.timego.ui.exercise

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.guidance.ExerciseGuidance

data class ExerciseDetailModel(
    val title: String,
    val subtitle: String,
    val equipment: List<String>,
    val purpose: String?,
    val setup: List<String>,
    val steps: List<String>,
    val cues: List<String>,
    val mistakes: List<String>,
    val easierVariationKey: String?,
    val personalSummary: String?,
    val isReviewed: Boolean,
)

fun buildExerciseDetail(
    exercise: Exercise,
    guidance: ExerciseGuidance?,
    lastPerformance: String? = null,
    record: String? = null,
): ExerciseDetailModel {
    val personal = listOfNotNull(lastPerformance?.let { "Last: $it" }, record?.let { "Best: $it" }).joinToString(" • ").ifBlank { null }
    if (exercise.isCustom || guidance == null) {
        return ExerciseDetailModel(
            title = exercise.name,
            subtitle = if (exercise.isCustom) "Custom exercise" else "Exercise details are not available yet",
            equipment = emptyList(), purpose = null, setup = emptyList(), steps = emptyList(), cues = emptyList(), mistakes = emptyList(),
            easierVariationKey = null, personalSummary = personal, isReviewed = false,
        )
    }
    return ExerciseDetailModel(
        title = exercise.name,
        subtitle = if (guidance.isReviewedComplete) "${guidance.difficulty.lowercase().replaceFirstChar(Char::uppercase)} • ${guidance.complexity.lowercase().replaceFirstChar(Char::uppercase)} complexity" else "Guidance in progress",
        equipment = guidance.equipment,
        purpose = guidance.purpose.ifBlank { null },
        setup = guidance.setup,
        steps = guidance.steps,
        cues = guidance.cues,
        mistakes = guidance.mistakes,
        easierVariationKey = guidance.easierVariationKey,
        personalSummary = personal,
        isReviewed = guidance.isReviewedComplete,
    )
}
