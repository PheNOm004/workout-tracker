package com.lsing.timego.domain

enum class EscalationTier { FULL, PARTIAL, HOLD }

/** RPE <=7 (low effort) -> FULL; RPE 8 (moderate) -> PARTIAL; RPE >=9 (near/at failure) -> HOLD;
 *  null (not logged) -> FULL, same as the unconditional-escalate behavior used when there's no
 *  effort signal to gate on. Framed as fuzzy membership over RPE (low/moderate/max effort) but
 *  defuzzified to one of these three discrete, inspectable tiers -- no continuous model, no
 *  training data. */
fun escalationTierForRpe(rpe: Int?): EscalationTier = when {
    rpe == null || rpe <= 7 -> EscalationTier.FULL
    rpe == 8 -> EscalationTier.PARTIAL
    else -> EscalationTier.HOLD
}
