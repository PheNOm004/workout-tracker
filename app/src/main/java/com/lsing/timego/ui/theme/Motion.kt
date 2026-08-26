package com.lsing.timego.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset

/** Shared motion vocabulary for the Engine-Room Gauge Panel. Every spec below is a spring, not a
 *  fixed-duration tween: the panel's motion signature is mechanical inertia -- a needle, a
 *  toggle, or a screen settles into place with a little weight and a light overshoot, never a
 *  hard cut. `dialSweep` carries the most weight (a real needle read) and is the slowest; UI
 *  chrome transitions are quicker but never snap. */
object TimeGoMotion {
    val expandEnter: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val expandExit: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val fadeEnter: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val fadeExit: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val contentEnter: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val contentExit: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val navigationIn: FiniteAnimationSpec<Int> = spring(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val navigationOut: FiniteAnimationSpec<Int> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val navigationInOffset: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val navigationOutOffset: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val pulseWidth: FiniteAnimationSpec<androidx.compose.ui.unit.Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** A gauge needle sweeping to a new reading: real inertia, a light overshoot past the target
     *  before it settles -- the app's one signature motion, reserved for GaugeDial and other
     *  literal instrument readings, never used for generic screen chrome. */
    val dialSweep: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.62f,
        stiffness = 90f,
    )
}
