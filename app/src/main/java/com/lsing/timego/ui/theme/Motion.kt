package com.lsing.timego.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Shared motion vocabulary for the Engine-Room Gauge Panel.
 *  Screen chrome, tabs, and expansions use symmetric EaseInOut easing both ways (enter and exit)
 *  for a smooth mechanical glide matching the heatmap scrolling, while needle readings preserve
 *  instrument inertia via [dialSweep]. */
object TimeGoMotion {
    val expandEnter: FiniteAnimationSpec<IntSize> = tween(
        durationMillis = 280,
        easing = EaseInOut,
    )
    val expandExit: FiniteAnimationSpec<IntSize> = tween(
        durationMillis = 280,
        easing = EaseInOut,
    )
    val fadeEnter: FiniteAnimationSpec<Float> = tween(
        durationMillis = 240,
        easing = EaseInOut,
    )
    val fadeExit: FiniteAnimationSpec<Float> = tween(
        durationMillis = 240,
        easing = EaseInOut,
    )
    val contentEnter: FiniteAnimationSpec<Float> = tween(
        durationMillis = 280,
        easing = EaseInOut,
    )
    val contentExit: FiniteAnimationSpec<Float> = tween(
        durationMillis = 280,
        easing = EaseInOut,
    )
    val navigationIn: FiniteAnimationSpec<Int> = tween(
        durationMillis = 300,
        easing = EaseInOut,
    )
    val navigationOut: FiniteAnimationSpec<Int> = tween(
        durationMillis = 300,
        easing = EaseInOut,
    )
    val navigationInOffset: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = 300,
        easing = EaseInOut,
    )
    val navigationOutOffset: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = 300,
        easing = EaseInOut,
    )
    val pulseWidth: FiniteAnimationSpec<Dp> = tween(
        durationMillis = 300,
        easing = EaseInOut,
    )

    /** A gauge needle sweeping to a new reading: real inertia, a light overshoot past the target
     *  before it settles -- the app's one signature motion, reserved for GaugeDial and other
     *  literal instrument readings, never used for generic screen chrome. */
    val dialSweep: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.62f,
        stiffness = 90f,
    )
}
