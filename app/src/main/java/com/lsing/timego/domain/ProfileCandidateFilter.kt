package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.profile.Equipment
import com.lsing.timego.profile.MovementLimitation
import com.lsing.timego.profile.TrainingProfile

data class CandidateMetadata(
    val equipment: Set<Equipment> = emptySet(),
    val stressRegions: Set<MovementLimitation> = emptySet(),
)

data class CandidateFilterResult(
    val candidates: List<Exercise>,
    val exclusionReasons: Map<Long, String>,
    val abstentionReason: String? = null,
)

fun filterCandidates(
    exercises: List<Exercise>,
    profile: TrainingProfile,
    metadataByCatalogueKey: Map<String, CandidateMetadata>,
): CandidateFilterResult {
    if (!profile.isComplete) return CandidateFilterResult(exercises, emptyMap())

    val exclusions = linkedMapOf<Long, String>()
    val candidates = exercises.filter { exercise ->
        val metadata = exercise.catalogueKey?.let(metadataByCatalogueKey::get) ?: return@filter true
        when {
            metadata.equipment.isNotEmpty() && !profile.equipment.containsAll(metadata.equipment) -> {
                exclusions[exercise.id] = "Requires equipment outside your profile."
                false
            }
            metadata.stressRegions.any { it in profile.movementLimitations } -> {
                exclusions[exercise.id] = "Conflicts with a movement preference in your profile."
                false
            }
            else -> true
        }
    }
    return CandidateFilterResult(
        candidates = candidates,
        exclusionReasons = exclusions,
        abstentionReason = if (exercises.isNotEmpty() && candidates.isEmpty()) {
            "No exercises match your equipment and movement preferences."
        } else {
            null
        },
    )
}
