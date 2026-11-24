package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog

/** A tag on a hold that makes it easier than the clean version: a resistance band or assistance in kg. */
data class LeverageModifier(val kind: String, val value: String, val label: String)

object LeverageKind {
    const val BAND = "BAND"
    const val ASSIST_KG = "ASSIST_KG"
}

val LEVERAGE_MODIFIER_PRESETS: List<LeverageModifier> = listOf(
    LeverageModifier(LeverageKind.BAND, "LIGHT", "Light band"),
    LeverageModifier(LeverageKind.BAND, "MEDIUM", "Medium band"),
    LeverageModifier(LeverageKind.BAND, "HEAVY", "Heavy band"),
    LeverageModifier(LeverageKind.ASSIST_KG, "5", "Assisted 5 kg"),
    LeverageModifier(LeverageKind.ASSIST_KG, "10", "Assisted 10 kg"),
    LeverageModifier(LeverageKind.ASSIST_KG, "15", "Assisted 15 kg"),
)

/** Display label for a stored modifier pair, or null for a clean set. */
fun modifierLabel(kind: String?, value: String?): String? {
    if (kind == null || value == null) return null
    return LEVERAGE_MODIFIER_PRESETS.firstOrNull { it.kind == kind && it.value == value }?.label ?: "$kind $value"
}

/** A hold is "clean" when it carries no easier-variation tag. Only clean holds count toward records and the main curve. */
fun isCleanSet(log: SetLog): Boolean = log.modifierKind == null

data class AssistedBest(val label: String, val seconds: Int)

/** The longest working hold for each modifier on one exercise, longest first, so an assisted hold stays visible without
 *  ever replacing the clean record. */
fun assistedHoldBests(exercise: Exercise, history: List<SetLog>): List<AssistedBest> {
    if (exercise.loggingType != LoggingType.HOLD.name) return emptyList()
    return history
        .filter { !it.isWarmup && !isCleanSet(it) && (it.holdSeconds ?: 0) > 0 }
        .groupBy { modifierLabel(it.modifierKind, it.modifierValue)!! }
        .map { (label, sets) -> AssistedBest(label, sets.maxOf { it.holdSeconds ?: 0 }) }
        .sortedByDescending { it.seconds }
}
