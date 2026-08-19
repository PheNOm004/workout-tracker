package com.lsing.timego.domain

import com.lsing.timego.data.MuscleGroup

/** Intensity to render a diagram shape tagged [group] with, given per-group [intensities] (0f..1f,
 *  missing key treated as untrained/0f -- same shape as [RadarChart]'s input). Delt artwork is
 *  kept tied to its own head: averaging front/side/rear delts made a front-delt session color
 *  unrelated shoulder regions. FULL_BODY is a fallback for anatomical shapes because the catch-all
 *  group has no literal SVG shape of its own. */
fun diagramZoneIntensity(group: MuscleGroup, intensities: Map<String, Float>): Float =
    maxOf(intensities[group.name] ?: 0f, intensities[MuscleGroup.FULL_BODY.name] ?: 0f)

/** Expands the catch-all FULL_BODY tag into every drawable anatomical group for cropped session
 *  diagrams. The original tag remains in the exercise/session data for labels and analytics. */
fun diagramGroupsForHeatmap(groups: Set<String>): Set<String> =
    if (MuscleGroup.FULL_BODY.name in groups) {
        groups + MuscleGroup.entries.filterNot { it == MuscleGroup.FULL_BODY }.map { it.name }
    } else {
        groups
    }

private val UPPER_BODY_CROP_GROUPS = setOf(
    MuscleGroup.CHEST.name,
    MuscleGroup.LATS.name,
    MuscleGroup.UPPER_BACK.name,
    MuscleGroup.LOWER_BACK.name,
    MuscleGroup.TRAPS.name,
    MuscleGroup.FRONT_DELTS.name,
    MuscleGroup.SIDE_DELTS.name,
    MuscleGroup.REAR_DELTS.name,
    MuscleGroup.BICEPS.name,
    MuscleGroup.TRICEPS.name,
    MuscleGroup.FOREARMS.name,
    MuscleGroup.ABS.name,
    MuscleGroup.OBLIQUES.name,
)

private val LOWER_BODY_CROP_GROUPS = setOf(
    MuscleGroup.QUADS.name,
    MuscleGroup.HAMSTRINGS.name,
    MuscleGroup.GLUTES.name,
    MuscleGroup.ADDUCTORS.name,
    MuscleGroup.CALVES.name,
)

/** Selects only the anatomical context needed to size a recommendation crop. */
fun diagramGroupsForRecommendationCrop(groups: Set<String>): Set<String> {
    val drawableGroups = diagramGroupsForHeatmap(groups)
    val hasUpperBody = drawableGroups.any { it in UPPER_BODY_CROP_GROUPS }
    val hasLowerBody = drawableGroups.any { it in LOWER_BODY_CROP_GROUPS }
    return when {
        hasUpperBody && !hasLowerBody -> UPPER_BODY_CROP_GROUPS
        hasLowerBody && !hasUpperBody -> LOWER_BODY_CROP_GROUPS
        else -> drawableGroups
    }
}
