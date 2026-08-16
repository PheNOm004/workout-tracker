package com.lsing.timego.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/** Shared motion vocabulary for the Night Training Console. */
object TimeGoMotion {
    val expandEnter: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.9f,
        stiffness = 420f,
    )
    val expandExit: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )
    val fadeEnter: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val fadeExit: FiniteAnimationSpec<Float> = tween(durationMillis = 140)
    val contentEnter: FiniteAnimationSpec<Float> = tween(durationMillis = 240)
    val contentExit: FiniteAnimationSpec<Float> = tween(durationMillis = 160)
    val navigationIn: FiniteAnimationSpec<Int> = tween(durationMillis = 260)
    val navigationOut: FiniteAnimationSpec<Int> = tween(durationMillis = 180)
    val pulseWidth: FiniteAnimationSpec<androidx.compose.ui.unit.Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}
