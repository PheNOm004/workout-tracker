package com.lsing.timego.domain.programs

import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.ai.MovementPattern

private fun g(vararg groups: MuscleGroup): Set<String> = groups.map { it.name }.toSet()

private val PUSH_REGIONS = g(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS)
private val PULL_REGIONS = g(
    MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.TRAPS,
    MuscleGroup.BICEPS, MuscleGroup.FOREARMS,
)
private val LEGS_REGIONS = g(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.ADDUCTORS)
private val CORE_REGIONS = g(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
private val SHOULDERS_ARMS_REGIONS = g(
    MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS,
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS,
)
private val UPPER_REGIONS = PUSH_REGIONS + PULL_REGIONS + g(MuscleGroup.REAR_DELTS)
private val ALL_REGIONS = MuscleGroup.entries.filterNot { it == MuscleGroup.FULL_BODY }.map { it.name }.toSet()

private val fullBody = Program(
    id = "full_body",
    name = "Full Body",
    dayTypes = listOf(
        ProgramDayType(
            name = "Full Body",
            regionGroups = ALL_REGIONS,
            slots = listOf(
                ProgramSlot("Squat pattern", LEGS_REGIONS),
                ProgramSlot("Push pattern", PUSH_REGIONS),
                ProgramSlot("Pull pattern", PULL_REGIONS),
                ProgramSlot("Core", CORE_REGIONS),
            ),
        ),
    ),
)

private val ppl = Program(
    id = "ppl",
    name = "Push / Pull / Legs",
    dayTypes = listOf(
        ProgramDayType(
            name = "Push",
            regionGroups = PUSH_REGIONS,
            slots = listOf(
                ProgramSlot("Main press", g(MuscleGroup.CHEST)),
                ProgramSlot("Overhead press", g(MuscleGroup.FRONT_DELTS)),
                ProgramSlot("Lateral raise", g(MuscleGroup.SIDE_DELTS)),
                ProgramSlot("Triceps isolation", g(MuscleGroup.TRICEPS)),
            ),
        ),
        ProgramDayType(
            name = "Pull",
            regionGroups = PULL_REGIONS,
            slots = listOf(
                ProgramSlot("Main pull", g(MuscleGroup.LATS)),
                ProgramSlot("Row", g(MuscleGroup.UPPER_BACK)),
                ProgramSlot("Rear delt / face pull", g(MuscleGroup.TRAPS)),
                ProgramSlot("Biceps isolation", g(MuscleGroup.BICEPS)),
            ),
        ),
        ProgramDayType(
            name = "Legs",
            regionGroups = LEGS_REGIONS,
            slots = listOf(
                ProgramSlot("Main squat/hinge", g(MuscleGroup.QUADS)),
                ProgramSlot("Posterior chain", g(MuscleGroup.HAMSTRINGS)),
                ProgramSlot("Glute isolation", g(MuscleGroup.GLUTES)),
                ProgramSlot("Calf raise", g(MuscleGroup.CALVES)),
            ),
        ),
    ),
)

private val upperLower = Program(
    id = "upper_lower",
    name = "Upper / Lower",
    dayTypes = listOf(
        ProgramDayType(
            name = "Upper",
            regionGroups = UPPER_REGIONS,
            slots = listOf(
                ProgramSlot("Press", g(MuscleGroup.CHEST)),
                ProgramSlot("Row", g(MuscleGroup.UPPER_BACK)),
                ProgramSlot("Overhead press", g(MuscleGroup.FRONT_DELTS)),
                ProgramSlot("Vertical pull", g(MuscleGroup.LATS)),
            ),
        ),
        ProgramDayType(
            name = "Lower",
            regionGroups = LEGS_REGIONS + CORE_REGIONS,
            slots = listOf(
                ProgramSlot("Squat", g(MuscleGroup.QUADS)),
                ProgramSlot("Hinge", g(MuscleGroup.HAMSTRINGS)),
                ProgramSlot("Unilateral", g(MuscleGroup.GLUTES)),
                ProgramSlot("Core", CORE_REGIONS),
            ),
        ),
    ),
)

private val broSplit = Program(
    id = "bro_split",
    name = "Bro Split",
    dayTypes = listOf(
        ProgramDayType("Chest", g(MuscleGroup.CHEST), listOf(ProgramSlot("Chest", g(MuscleGroup.CHEST)))),
        ProgramDayType(
            "Back",
            g(MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.TRAPS),
            listOf(ProgramSlot("Back", g(MuscleGroup.LATS, MuscleGroup.UPPER_BACK))),
        ),
        ProgramDayType(
            "Shoulders",
            g(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS),
            listOf(ProgramSlot("Shoulders", g(MuscleGroup.SIDE_DELTS))),
        ),
        ProgramDayType(
            "Arms",
            g(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS),
            listOf(ProgramSlot("Biceps", g(MuscleGroup.BICEPS)), ProgramSlot("Triceps", g(MuscleGroup.TRICEPS))),
        ),
        ProgramDayType("Legs", LEGS_REGIONS, listOf(ProgramSlot("Legs", g(MuscleGroup.QUADS)))),
    ),
)

private val arnold = Program(
    id = "arnold",
    name = "Arnold Split",
    dayTypes = listOf(
        ProgramDayType(
            "Chest & Back",
            g(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK),
            listOf(ProgramSlot("Chest", g(MuscleGroup.CHEST)), ProgramSlot("Back", g(MuscleGroup.LATS))),
        ),
        ProgramDayType(
            "Shoulders & Arms",
            SHOULDERS_ARMS_REGIONS,
            listOf(ProgramSlot("Shoulders", g(MuscleGroup.SIDE_DELTS)), ProgramSlot("Arms", g(MuscleGroup.BICEPS, MuscleGroup.TRICEPS))),
        ),
        ProgramDayType("Legs", LEGS_REGIONS, listOf(ProgramSlot("Legs", g(MuscleGroup.QUADS)))),
    ),
)

private val calisthenicsProgression = Program(
    id = "calisthenics_progression",
    name = "Calisthenics Progression",
    dayTypes = listOf(
        // Beginner tier
        ProgramDayType(
            name = "Beginner Full Body",
            regionGroups = ALL_REGIONS,
            slots = listOf(
                ProgramSlot("Push", PUSH_REGIONS),
                ProgramSlot("Pull", PULL_REGIONS),
                ProgramSlot("Squat", LEGS_REGIONS),
                ProgramSlot("Core", CORE_REGIONS),
            ),
        ),
        // Intermediate tier
        ProgramDayType("Intermediate Push", PUSH_REGIONS, listOf(ProgramSlot("Push", PUSH_REGIONS))),
        ProgramDayType("Intermediate Pull", PULL_REGIONS, listOf(ProgramSlot("Pull", PULL_REGIONS))),
        ProgramDayType("Intermediate Legs", LEGS_REGIONS, listOf(ProgramSlot("Legs", LEGS_REGIONS))),
        // Advanced tier -- straight-arm slots route through BiomechanicalRegistry (ProgramSlotResolver)
        ProgramDayType(
            name = "Advanced Bent-Arm",
            regionGroups = PULL_REGIONS + PUSH_REGIONS,
            slots = listOf(
                ProgramSlot("Pull-up/row work", PULL_REGIONS, movementPattern = MovementPattern.VERTICAL_PULL),
                ProgramSlot("Dip work", PUSH_REGIONS, movementPattern = MovementPattern.HORIZONTAL_PUSH),
            ),
        ),
        ProgramDayType(
            name = "Advanced Straight-Arm",
            regionGroups = PULL_REGIONS + g(MuscleGroup.FRONT_DELTS),
            slots = listOf(
                ProgramSlot("Lever/skill work", PULL_REGIONS, movementPattern = MovementPattern.HORIZONTAL_PULL),
                ProgramSlot("Handstand/press work", g(MuscleGroup.FRONT_DELTS), movementPattern = MovementPattern.VERTICAL_PUSH),
            ),
        ),
        ProgramDayType(
            "Advanced Legs & Mobility",
            LEGS_REGIONS,
            listOf(ProgramSlot("Legs", LEGS_REGIONS, movementPattern = MovementPattern.KNEE_DOMINANT)),
        ),
    ),
)

object ProgramRegistry {
    val ALL: List<Program> = listOf(fullBody, ppl, upperLower, broSplit, arnold, calisthenicsProgression)

    fun byId(id: String?): Program? = ALL.firstOrNull { it.id == id }
}
