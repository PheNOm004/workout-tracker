package com.lsing.timego.data

/** One seeded routine belonging to a Program. [exerciseNames] are matched against the already-seeded
 *  exercise catalogue by exact name (see [WorkoutRepository.seedMissingRoutines]) -- any name with no
 *  catalogue match is silently skipped rather than failing the whole routine, so a future catalogue
 *  rename doesn't brick seeding. */
data class SeedRoutine(
    val programId: String,
    val name: String,
    val tier: String? = null,
    val exerciseNames: List<String>,
)

/** Predetermined, curated Program routines -- sourced from reddit-PPL/upper-lower-split research and
 *  matched to this catalogue's exact exercise names. Static content, not user data: same shape and
 *  seeding convention as [SEED_EXERCISES]. Selecting a Program on the landing page filters "Your
 *  Routines" to the routines whose [SeedRoutine.programId] matches -- it never generates or deletes
 *  routines at selection time, only at first-launch seeding. */
val SEED_ROUTINES: List<SeedRoutine> = listOf(
    // Full Body -- StrongLifts-style A/B alternation
    SeedRoutine("full_body", "Full Body A", exerciseNames = listOf("Barbell Back Squat", "Barbell Bench Press", "Barbell Row")),
    SeedRoutine("full_body", "Full Body B", exerciseNames = listOf("Barbell Back Squat", "Overhead Press", "Romanian Deadlift")),

    // Push / Pull / Legs
    SeedRoutine("ppl", "Push", exerciseNames = listOf("Barbell Bench Press", "Overhead Press", "Lateral Raise", "Tricep Pushdown")),
    SeedRoutine("ppl", "Pull", exerciseNames = listOf("Barbell Row", "Lat Pulldown", "Face Pull", "Barbell Curl")),
    SeedRoutine("ppl", "Legs", exerciseNames = listOf("Barbell Back Squat", "Romanian Deadlift", "Leg Press", "Standing Calf Raise")),

    // Upper / Lower
    SeedRoutine("upper_lower", "Upper", exerciseNames = listOf("Barbell Bench Press", "Barbell Row", "Overhead Press", "Lat Pulldown")),
    SeedRoutine("upper_lower", "Lower", exerciseNames = listOf("Barbell Back Squat", "Romanian Deadlift", "Bulgarian Split Squat", "Plank")),

    // Bro Split
    SeedRoutine("bro_split", "Chest Day", exerciseNames = listOf("Barbell Bench Press", "Incline Dumbbell Press")),
    SeedRoutine("bro_split", "Back Day", exerciseNames = listOf("Barbell Row", "Lat Pulldown")),
    SeedRoutine("bro_split", "Shoulder Day", exerciseNames = listOf("Overhead Press", "Lateral Raise")),
    SeedRoutine("bro_split", "Arm Day", exerciseNames = listOf("Barbell Curl", "Tricep Pushdown")),
    SeedRoutine("bro_split", "Leg Day", exerciseNames = listOf("Barbell Back Squat", "Leg Press")),

    // Arnold Split
    SeedRoutine("arnold", "Chest & Back", exerciseNames = listOf("Barbell Bench Press", "Barbell Row")),
    SeedRoutine("arnold", "Shoulders & Arms", exerciseNames = listOf("Overhead Press", "Barbell Curl", "Tricep Pushdown")),
    SeedRoutine("arnold", "Legs", exerciseNames = listOf("Barbell Back Squat", "Leg Curl")),

    // Calisthenics Progression -- tiered
    SeedRoutine("calisthenics_progression", "Beginner Full Body", tier = "BEGINNER", exerciseNames = listOf("Push-Up", "Australian Pull-Up", "Step-Up", "Plank")),
    SeedRoutine("calisthenics_progression", "Intermediate Push", tier = "INTERMEDIATE", exerciseNames = listOf("Push-Up", "Dip")),
    SeedRoutine("calisthenics_progression", "Intermediate Pull", tier = "INTERMEDIATE", exerciseNames = listOf("Pull-Up", "Australian Pull-Up")),
    SeedRoutine("calisthenics_progression", "Intermediate Legs", tier = "INTERMEDIATE", exerciseNames = listOf("Bulgarian Split Squat", "Assisted Pistol Squat")),
    SeedRoutine("calisthenics_progression", "Advanced Bent-Arm", tier = "ADVANCED", exerciseNames = listOf("Pull-Up", "Ring Dip")),
    SeedRoutine("calisthenics_progression", "Advanced Straight-Arm", tier = "ADVANCED", exerciseNames = listOf("Tuck Front Lever", "Wall Handstand Hold")),
    SeedRoutine("calisthenics_progression", "Advanced Legs & Mobility", tier = "ADVANCED", exerciseNames = listOf("Pistol Squat")),
)

/** Display names for the Program picker, keyed by [SeedRoutine.programId]. */
val PROGRAM_DISPLAY_NAMES: Map<String, String> = mapOf(
    "full_body" to "Full Body",
    "ppl" to "Push / Pull / Legs",
    "upper_lower" to "Upper / Lower",
    "bro_split" to "Bro Split",
    "arnold" to "Arnold Split",
    "calisthenics_progression" to "Calisthenics Progression",
)
