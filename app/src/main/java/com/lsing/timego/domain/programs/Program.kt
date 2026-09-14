package com.lsing.timego.domain.programs

import com.lsing.timego.domain.ai.MovementPattern

/** One exercise slot within a [ProgramDayType]. [targetGroups] uses the same MuscleGroup-name
 *  convention as [com.lsing.timego.domain.suggestedExerciseFor]'s targetGroups parameter.
 *  [movementPattern] is set only on Advanced-calisthenics straight-arm slots, where resolution
 *  routes through [com.lsing.timego.domain.ai.BiomechanicalRegistry] instead of the default
 *  least-used-exercise path -- see ProgramSlotResolver. */
data class ProgramSlot(
    val label: String,
    val targetGroups: Set<String>,
    val movementPattern: MovementPattern? = null,
)

/** A named training day within a [Program] (e.g. "Push", "Beginner Full Body"). [regionGroups] is
 *  the union of every slot's target muscle groups, used by [com.lsing.timego.domain.recommendProgramDayType]
 *  to score this day-type the same way a single region is scored today. */
data class ProgramDayType(
    val name: String,
    val regionGroups: Set<String>,
    val slots: List<ProgramSlot>,
)

/** A curated, static split template. [id] is a stable string key (e.g. "ppl") used for
 *  [com.lsing.timego.data.SettingsRepository.activeProgramId] persistence -- never a Room row id. */
data class Program(
    val id: String,
    val name: String,
    val dayTypes: List<ProgramDayType>,
)
