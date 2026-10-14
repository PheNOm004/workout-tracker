package com.lsing.timego.data

/** Per-exercise prescription for a seeded program, shown as placeholders while logging.
 *  Intensity is stored as target RPE (RPE = 10 - RIR), matching the RPE field in the log UI. */
data class Prescription(
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val restMinSec: Int,
    val restMaxSec: Int,
    val rpeLow: Int,
    val rpeHigh: Int,
    val tempo: String,
    val note: String? = null,
) {
    val rpeText get() = "$rpeLow-$rpeHigh"
    val summary get() = buildString {
        append("$sets x $repMin-$repMax")
        append(" · RPE $rpeText")
        append(" · rest ${restMinSec / 60}-${restMaxSec / 60} min")
        append(" · tempo $tempo")
    }
}

const val RIR_PROGRAM_ID = "rir_upper_lower"

private const val POWER_TEMPO = "2,1,1,1"
private const val CONTROLLED = "controlled"

private fun heavy(sets: Int, reps: IntRange, note: String? = null) =
    Prescription(sets, reps.first, reps.last, 120, 180, 7, 9, POWER_TEMPO, note)

private fun accessory(sets: Int, note: String? = null) =
    Prescription(sets, 10, 15, 60, 120, 6, 8, CONTROLLED, note)

/** Keyed by catalogue exercise name; every name is unique within [RIR_PROGRAM_ID]. */
val RIR_PROGRAM_PRESCRIPTIONS: Map<String, Prescription> = mapOf(
    "Pull-Up" to heavy(3, 6..10, "Progress with weighted variations"),
    "Dip" to heavy(3, 6..10, "Progress with weighted variations"),
    "Chest-Supported Row" to heavy(2, 8..12),
    "Seated Dumbbell Shoulder Press" to heavy(2, 8..12),
    "Preacher Curl" to accessory(2),
    "Lateral Raise" to accessory(2),

    "Hack Squat" to heavy(3, 6..10),
    "Nordic Hamstring Curl" to heavy(3, 6..10, "Start with eccentrics, progress from there"),
    "Leg Extension" to heavy(2, 8..12),
    "Hyperextension" to heavy(2, 8..12),
    "Hanging Knee Raise" to accessory(2, "Captain's chair knee tuck; progress with Hanging Leg Raises"),
    "Ab Wheel Rollout" to accessory(2, "Progress with increased ROM"),

    "Incline Dumbbell Press" to heavy(3, 6..10),
    "T-Bar Row" to heavy(3, 6..10),
    "Pec Deck Machine" to heavy(2, 8..12),
    "Rear Delt Fly" to heavy(2, 8..12),
    "Hammer Curl" to accessory(2),
    "Tricep Pushdown" to accessory(2),

    "Pistol Squat" to heavy(3, 6..10, "Assisted first, progress gradually"),
    "Romanian Deadlift" to heavy(3, 6..10),
    "Leg Press" to heavy(2, 8..12),
    "Standing Calf Raise" to heavy(2, 8..12),
    "Cable Kneeling Crunch" to accessory(2),
    "Dumbbell Side Bend" to accessory(2),
)
