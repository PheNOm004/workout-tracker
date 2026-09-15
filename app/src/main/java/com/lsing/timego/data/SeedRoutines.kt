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

/** Predetermined, curated Program routines -- researched per program against its real published
 *  structure (StrongLifts, standard PPL/Upper-Lower/Bro-Split/Arnold splits, PHUL, PHAT, GZCLP,
 *  Westside Conjugate) and matched to this catalogue's exact exercise names. Static content, not user
 *  data: same shape and seeding convention as [SEED_EXERCISES]. Selecting a Program on the landing page
 *  filters "Your Routines" to the routines whose [SeedRoutine.programId] matches -- it never generates
 *  or deletes routines at selection time, only at first-launch seeding. */
val SEED_ROUTINES: List<SeedRoutine> = listOf(
    // Full Body -- StrongLifts-style A/B alternation
    SeedRoutine("full_body", "Full Body A", exerciseNames = listOf("Barbell Back Squat", "Barbell Bench Press", "Barbell Row")),
    SeedRoutine("full_body", "Full Body B", exerciseNames = listOf("Barbell Back Squat", "Overhead Press", "Conventional Deadlift")),

    // Push / Pull / Legs
    SeedRoutine("ppl", "Push", exerciseNames = listOf("Barbell Bench Press", "Overhead Press", "Incline Dumbbell Press", "Lateral Raise", "Tricep Pushdown", "Skull Crusher")),
    SeedRoutine("ppl", "Pull", exerciseNames = listOf("Barbell Row", "Lat Pulldown", "Seated Cable Row", "Face Pull", "Barbell Curl", "Hammer Curl")),
    SeedRoutine("ppl", "Legs", exerciseNames = listOf("Barbell Back Squat", "Romanian Deadlift", "Leg Press", "Leg Curl", "Leg Extension", "Standing Calf Raise")),

    // Upper / Lower
    SeedRoutine("upper_lower", "Upper", exerciseNames = listOf("Barbell Bench Press", "Barbell Row", "Overhead Press", "Lat Pulldown", "Barbell Curl", "Tricep Pushdown")),
    SeedRoutine("upper_lower", "Lower", exerciseNames = listOf("Barbell Back Squat", "Romanian Deadlift", "Bulgarian Split Squat", "Leg Curl", "Standing Calf Raise", "Plank")),

    // Bro Split
    SeedRoutine("bro_split", "Chest Day", exerciseNames = listOf("Barbell Bench Press", "Incline Dumbbell Press", "Dumbbell Fly", "Cable Crossover", "Weighted Chest Dip")),
    SeedRoutine("bro_split", "Back Day", exerciseNames = listOf("Barbell Row", "Lat Pulldown", "Seated Cable Row", "T-Bar Row", "Face Pull")),
    SeedRoutine("bro_split", "Shoulder Day", exerciseNames = listOf("Overhead Press", "Lateral Raise", "Rear Delt Fly", "Cable Lateral Raise", "Dumbbell Shrug")),
    SeedRoutine("bro_split", "Arm Day", exerciseNames = listOf("Barbell Curl", "Hammer Curl", "Preacher Curl", "Tricep Pushdown", "Skull Crusher", "Close-Grip Bench Press")),
    SeedRoutine("bro_split", "Leg Day", exerciseNames = listOf("Barbell Back Squat", "Leg Press", "Leg Curl", "Leg Extension", "Standing Calf Raise")),

    // Arnold Split -- three workouts, each hit twice a week
    SeedRoutine("arnold", "Chest & Back", exerciseNames = listOf("Barbell Bench Press", "Incline Dumbbell Press", "Barbell Row", "Lat Pulldown", "Weighted Chest Dip", "Seated Cable Row")),
    SeedRoutine("arnold", "Shoulders & Arms", exerciseNames = listOf("Overhead Press", "Lateral Raise", "Barbell Curl", "Tricep Pushdown", "Skull Crusher", "Hammer Curl")),
    SeedRoutine("arnold", "Legs", exerciseNames = listOf("Barbell Back Squat", "Leg Press", "Leg Curl", "Leg Extension", "Standing Calf Raise")),

    // PHUL (Power Hypertrophy Upper Lower) -- 4-day powerbuilding split
    SeedRoutine("phul", "Upper Power", exerciseNames = listOf("Barbell Bench Press", "Barbell Row", "Overhead Press", "Weighted Pull-Up")),
    SeedRoutine("phul", "Lower Power", exerciseNames = listOf("Barbell Back Squat", "Conventional Deadlift", "Leg Press")),
    SeedRoutine("phul", "Upper Hypertrophy", exerciseNames = listOf("Incline Dumbbell Press", "Seated Cable Row", "Lateral Raise", "Barbell Curl", "Tricep Pushdown")),
    SeedRoutine("phul", "Lower Hypertrophy", exerciseNames = listOf("Leg Extension", "Leg Curl", "Romanian Deadlift", "Standing Calf Raise")),

    // PHAT (Power Hypertrophy Adaptive Training) -- Layne Norton's 5-day powerbuilding split
    SeedRoutine("phat", "Upper Power", exerciseNames = listOf("Barbell Bench Press", "Barbell Row", "Weighted Pull-Up", "Overhead Press")),
    SeedRoutine("phat", "Lower Power", exerciseNames = listOf("Barbell Back Squat", "Romanian Deadlift", "Leg Press")),
    SeedRoutine("phat", "Back & Shoulders Hypertrophy", exerciseNames = listOf("Lat Pulldown", "Seated Cable Row", "T-Bar Row", "Lateral Raise", "Face Pull")),
    SeedRoutine("phat", "Lower Hypertrophy", exerciseNames = listOf("Hack Squat", "Leg Extension", "Leg Curl", "Standing Calf Raise")),
    SeedRoutine("phat", "Chest & Arms Hypertrophy", exerciseNames = listOf("Incline Dumbbell Press", "Cable Crossover", "Barbell Curl", "Hammer Curl", "Tricep Pushdown", "Skull Crusher")),

    // GZCLP -- tiered linear progression, A1/B1/A2/B2 rotation, T1 heavy / T2 moderate / T3 accessory
    SeedRoutine("gzclp", "Workout A1", exerciseNames = listOf("Barbell Back Squat", "Barbell Bench Press", "Lat Pulldown")),
    SeedRoutine("gzclp", "Workout B1", exerciseNames = listOf("Overhead Press", "Conventional Deadlift", "Barbell Curl")),
    SeedRoutine("gzclp", "Workout A2", exerciseNames = listOf("Barbell Bench Press", "Barbell Back Squat", "Lat Pulldown")),
    SeedRoutine("gzclp", "Workout B2", exerciseNames = listOf("Conventional Deadlift", "Overhead Press", "Barbell Curl")),

    // Westside Conjugate -- max-effort/dynamic-effort split, upper/lower each rotated weekly
    SeedRoutine("westside_conjugate", "Max Effort Lower", exerciseNames = listOf("Safety Bar Squat", "Good Morning", "Reverse Hyperextension")),
    SeedRoutine("westside_conjugate", "Max Effort Upper", exerciseNames = listOf("Floor Press", "Close-Grip Bench Press", "Face Pull")),
    SeedRoutine("westside_conjugate", "Dynamic Effort Lower", exerciseNames = listOf("Box Squat", "Banded Box Squat", "Reverse Hyperextension")),
    SeedRoutine("westside_conjugate", "Dynamic Effort Upper", exerciseNames = listOf("Close-Grip Bench Press", "Skull Crusher", "Seated Cable Row")),

    // Calisthenics Progression -- tiered
    SeedRoutine("calisthenics_progression", "Beginner Full Body", tier = "BEGINNER", exerciseNames = listOf("Push-Up", "Australian Pull-Up", "Bodyweight Squat", "Step-Up", "Plank")),
    SeedRoutine("calisthenics_progression", "Intermediate Push", tier = "INTERMEDIATE", exerciseNames = listOf("Push-Up", "Dip", "Pike Push-Up", "Bench Dip")),
    SeedRoutine("calisthenics_progression", "Intermediate Pull", tier = "INTERMEDIATE", exerciseNames = listOf("Pull-Up", "Chin-Up", "Australian Pull-Up")),
    SeedRoutine("calisthenics_progression", "Intermediate Legs", tier = "INTERMEDIATE", exerciseNames = listOf("Bulgarian Split Squat", "Assisted Pistol Squat", "Bodyweight Calf Raise")),
    SeedRoutine("calisthenics_progression", "Advanced Bent-Arm", tier = "ADVANCED", exerciseNames = listOf("Weighted Pull-Up", "Ring Dip", "Muscle-Up")),
    SeedRoutine("calisthenics_progression", "Advanced Straight-Arm", tier = "ADVANCED", exerciseNames = listOf("Tuck Front Lever", "Tuck Planche Hold", "Wall Handstand Hold", "L-Sit")),
    SeedRoutine("calisthenics_progression", "Advanced Legs & Mobility", tier = "ADVANCED", exerciseNames = listOf("Pistol Squat", "Shrimp Squat", "Cossack Squat")),
)

/** Display names for the Program picker, keyed by [SeedRoutine.programId]. */
val PROGRAM_DISPLAY_NAMES: Map<String, String> = mapOf(
    "full_body" to "Full Body",
    "ppl" to "Push / Pull / Legs",
    "upper_lower" to "Upper / Lower",
    "bro_split" to "Bro Split",
    "arnold" to "Arnold Split",
    "phul" to "PHUL (Power Hypertrophy Upper Lower)",
    "phat" to "PHAT (Power Hypertrophy Adaptive Training)",
    "gzclp" to "GZCLP",
    "westside_conjugate" to "Westside Conjugate",
    "calisthenics_progression" to "Calisthenics Progression",
)
