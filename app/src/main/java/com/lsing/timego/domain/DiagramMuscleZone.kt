package com.lsing.timego.domain

import com.lsing.timego.data.MuscleGroup

private val SHOULDER_DELTS = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS)

/** Intensity to render a diagram shape tagged [group] with, given per-group [intensities] (0f..1f,
 *  missing key treated as untrained/0f -- same shape as [RadarChart]'s input). The anatomical
 *  heatmap diagram keeps front/side/rear delts as one combined "shoulders" visual zone since the
 *  traced source art has no clean boundary between them (see docs/superpowers/specs), so any of
 *  the three delt groups resolves to the average of all three instead of its own individual value;
 *  every other group resolves to its own value directly. */
fun diagramZoneIntensity(group: MuscleGroup, intensities: Map<String, Float>): Float =
    if (group in SHOULDER_DELTS) {
        SHOULDER_DELTS.map { intensities[it.name] ?: 0f }.average().toFloat()
    } else {
        intensities[group.name] ?: 0f
    }
