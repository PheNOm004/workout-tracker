package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.MuscleGroup
import kotlin.math.abs
import kotlin.math.sqrt

/** Exploratory "dumb coach" substitution relatedness -- see the 2026-09-05/06 vault brainstorm
 *  note (TimeGo/17 Dumb Coach Brainstorm). Deliberately separate from the reviewed
 *  adaptive-coach-catalogue and its promotion-gate contract: this is informal, ungated, and not
 *  wired into any UI. It only scores how related two exercises are as substitution candidates; it
 *  does not itself compute a difficulty ranking.
 *
 *  Family (push vs. pull) is a hard, categorical gate -- a push movement never relates to a pull
 *  movement, full stop, the same way [Exercise.loggingType] never mixes across reps/hold/duration
 *  bases. Within a family, "how inclined/overhead" is a continuous angle (0 degrees = fully
 *  horizontal, 90 = fully overhead/vertical) rather than a second hard category, so relatedness
 *  decays smoothly instead of an exercise either matching or being walled off entirely. */
enum class MovementFamily { PUSH, PULL, OTHER }

/** Checked in order for the same reason [derivePullAngle] checks "pull-up" before "row": longer,
 *  more specific keywords first so e.g. "Lat Pulldown" doesn't fall through to a generic match.
 *  [MovementFamily.OTHER] means the name didn't clearly indicate a family; an exercise that
 *  resolves to it never relates to anything (abstain on unknown, same stance as the rest of this
 *  project rather than guessing). Known gap, not yet resolved (see the brainstorm note's
 *  "explicitly deferred" section): pure keyword matching on the exercise name. */
fun deriveFamily(exerciseName: String): MovementFamily {
    val name = exerciseName.lowercase()
    val pushKeywords = listOf("push-up", "push up", "bench press", "chest press", "dip", "fly", "handstand", "pike", "overhead press", "shoulder press", "military press")
    val pullKeywords = listOf("pull-up", "pull up", "chin-up", "chin up", "pulldown", "pull down", "lat pull", "row")
    return when {
        pushKeywords.any { it in name } -> MovementFamily.PUSH
        pullKeywords.any { it in name } -> MovementFamily.PULL
        else -> MovementFamily.OTHER
    }
}

/** Per-[MuscleGroup] contribution, 0-100. A group in [Exercise.muscleGroups] but absent from
 *  [Exercise.muscleWeights] defaults to 100 (full credit), matching how the rest of the app reads
 *  that map (see the [Exercise] KDoc); a group the exercise doesn't target at all is 0. */
private fun muscleWeight(exercise: Exercise, group: MuscleGroup): Double =
    if (group.name in exercise.muscleGroups) (exercise.muscleWeights[group.name] ?: 100).toDouble() else 0.0

/** Push angle derived entirely from muscle-weight data already in the catalogue -- no per-exercise
 *  curation needed. Chest-dominant (flat/incline pushing) tags near-zero shoulder weight; fully
 *  overhead/inverted pushing (handstand push-up) tags zero chest weight and shoulder-only. The
 *  ratio between them approximates how far along that spectrum an exercise sits. Null only when an
 *  exercise tagged [MovementFamily.PUSH] has neither chest nor shoulder weight at all (shouldn't
 *  happen for a real push exercise, but abstain rather than divide by zero). */
private fun derivePushAngle(exercise: Exercise): Double? {
    val shoulder = muscleWeight(exercise, MuscleGroup.FRONT_DELTS) + muscleWeight(exercise, MuscleGroup.SIDE_DELTS)
    val chest = muscleWeight(exercise, MuscleGroup.CHEST)
    if (shoulder == 0.0 && chest == 0.0) return null
    return 90.0 * shoulder / (shoulder + chest)
}

/** Pull has no muscle-weight proxy for line-of-pull the way push has for incline: a Barbell Row
 *  and a Pull-Up tag nearly identical LATS/UPPER_BACK/TRAPS/BICEPS weights despite pulling in
 *  different directions relative to the torso. Falls back to a coarse keyword bucket instead --
 *  every pull-up/chin-up/pulldown variant is flatly 90 degrees, every row variant flatly 0, no
 *  in-between. Null (abstain) when the name matched [MovementFamily.PULL] via "row"/"pull" but
 *  neither bucket's specific keywords apply -- shouldn't happen given [deriveFamily]'s keyword
 *  list, but kept explicit rather than silently defaulting to one side. */
private fun derivePullAngle(exerciseName: String): Double? {
    val name = exerciseName.lowercase()
    return when {
        listOf("pull-up", "pull up", "chin-up", "chin up", "pulldown", "pull down", "lat pull").any { it in name } -> 90.0
        "row" in name -> 0.0
        else -> null
    }
}

/** Resolves an exercise to its (family, angle) if possible; null (abstain) if the family is
 *  unknown or its family-specific angle can't be derived. */
fun movementAngle(exercise: Exercise): Pair<MovementFamily, Double>? =
    when (val family = deriveFamily(exercise.name)) {
        MovementFamily.PUSH -> derivePushAngle(exercise)?.let { family to it }
        MovementFamily.PULL -> derivePullAngle(exercise.name)?.let { family to it }
        MovementFamily.OTHER -> null
    }

/** Cosine similarity of two exercises' muscle-contribution vectors, in [0, 1] since weights are
 *  never negative. 0 when either exercise has no tagged muscle groups at all. */
fun muscleSimilarity(a: Exercise, b: Exercise): Double {
    val groups = MuscleGroup.entries
    var dot = 0.0
    var magA = 0.0
    var magB = 0.0
    for (group in groups) {
        val wa = muscleWeight(a, group)
        val wb = muscleWeight(b, group)
        dot += wa * wb
        magA += wa * wa
        magB += wb * wb
    }
    if (magA == 0.0 || magB == 0.0) return 0.0
    return dot / (sqrt(magA) * sqrt(magB))
}

/** One candidate's graded relatedness to the exercise being substituted for. [relatedness] is
 *  [muscleSimilarity] scaled down by how far apart the two exercises sit on their shared family's
 *  angle axis (both angles run 0-90 within a family, so the maximum possible distance is 90) --
 *  same family and muscles but opposite ends of the incline spectrum trends toward 0 rather than
 *  being hard-excluded, and identical angle leaves similarity unscaled. */
data class SubstitutionCandidate(val exercise: Exercise, val muscleSimilarity: Double, val angleDistance: Double, val relatedness: Double)

/** Provisional -- not yet validated against the full ~1,000-exercise catalogue. Tune once real
 *  relatedness scores can be spot-checked rather than reasoned about from a handful of examples. */
const val DEFAULT_MIN_RELATEDNESS: Double = 0.1

/** Candidates [exercise] may be substituted with, most related first: same movement family, same
 *  [Exercise.loggingType] (reps/hold/duration bases are never mixed, per the adaptive-coach model
 *  card's evidence rules), and combined relatedness at or above [minRelatedness]. [exercise]
 *  itself is excluded from its own candidate list. Returns empty when [exercise] doesn't resolve
 *  to a known family/angle -- an unclassified exercise abstains rather than relating by chance. */
fun substitutionCandidatesFor(
    exercise: Exercise,
    catalogue: List<Exercise>,
    minRelatedness: Double = DEFAULT_MIN_RELATEDNESS,
): List<SubstitutionCandidate> {
    val (family, angle) = movementAngle(exercise) ?: return emptyList()
    return catalogue.mapNotNull { candidate ->
        if (candidate.id == exercise.id || candidate.loggingType != exercise.loggingType) return@mapNotNull null
        val (candidateFamily, candidateAngle) = movementAngle(candidate) ?: return@mapNotNull null
        if (candidateFamily != family) return@mapNotNull null
        val similarity = muscleSimilarity(exercise, candidate)
        val angleDistance = abs(angle - candidateAngle)
        val relatedness = similarity * (1.0 - angleDistance / 90.0)
        if (relatedness < minRelatedness) null else SubstitutionCandidate(candidate, similarity, angleDistance, relatedness)
    }.sortedByDescending { it.relatedness }
}
