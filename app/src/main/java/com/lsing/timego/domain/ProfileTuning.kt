package com.lsing.timego.domain

import com.lsing.timego.profile.TrainingGoal

/** Conventional rep range for the goal (common programming guidance, not individually validated), used to progress when there is no per-weight history yet. Null (keep the plain
 *  hit-target-then-add-weight rule) for goals without a characteristic range. */
fun goalRepRange(goal: TrainingGoal?): RepRange? = when (goal) {
    TrainingGoal.STRENGTH -> RepRange(floor = 3, ceiling = 6)
    TrainingGoal.HYPERTROPHY -> RepRange(floor = 6, ceiling = 12)
    TrainingGoal.ENDURANCE -> RepRange(floor = 12, ceiling = 20)
    else -> null
}
