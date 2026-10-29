package com.lsing.timego.data

/**
 * The owner's personal Upper/Lower AB (RIR) program. It lives in the debug source set only, so it is seeded on debug
 * builds and is not compiled into the public (release) app at all. Prescriptions are in RIR_PROGRAM_PRESCRIPTIONS.
 */
val PERSONAL_SEED_ROUTINES: List<SeedRoutine> = listOf(
    SeedRoutine(RIR_PROGRAM_ID, "Upper A", exerciseNames = listOf("Pull-Up", "Dip", "Chest-Supported Row", "Neutral-Grip Shoulder Press", "Preacher Curl", "Lateral Raise")),
    SeedRoutine(RIR_PROGRAM_ID, "Lower A", exerciseNames = listOf("Hack Squat", "Nordic Hamstring Curl", "Leg Extension", "Hyperextension", "Captain's Chair Knee Tuck", "Ab Wheel Rollout")),
    SeedRoutine(RIR_PROGRAM_ID, "Upper B", exerciseNames = listOf("Incline Dumbbell Press", "T-Bar Row", "Pec Deck Machine", "Rear Delt Fly", "Hammer Curl", "Tricep Pushdown")),
    SeedRoutine(RIR_PROGRAM_ID, "Lower B", exerciseNames = listOf("Pistol Squat", "Romanian Deadlift", "Leg Press", "Standing Calf Raise", "Weighted Crunch", "Roman Chair Side Crunch")),
)
