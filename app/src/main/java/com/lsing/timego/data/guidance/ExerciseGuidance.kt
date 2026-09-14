package com.lsing.timego.data.guidance

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_guidance")
data class ExerciseGuidance(
    @PrimaryKey val catalogueKey: String,
    val name: String,
    val catalogueVersion: Int,
    val aliases: List<String>,
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val difficulty: String,
    val complexity: String,
    val purpose: String,
    val setup: List<String>,
    val steps: List<String>,
    val cues: List<String>,
    val mistakes: List<String>,
    val easierVariationKey: String?,
    val harderVariationKeys: List<String>,
    val reviewStatus: String,
) {
    val isReviewedComplete: Boolean get() = reviewStatus == "REVIEWED_COMPLETE"
}
