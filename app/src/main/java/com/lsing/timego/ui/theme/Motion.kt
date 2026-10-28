package com.lsing.timego.ui.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Shared motion vocabulary. Things that move or resize use springs, so they carry momentum and stay
 * continuous when interrupted mid-flight (a tween restarts from zero velocity; a spring keeps it).
 * Opacity uses short asymmetric tweens: fade in decelerates, fade out accelerates, and exits are
 * faster than entrances so the UI never makes you wait to dismiss something.
 *
 * Damping sits just under critical (0.85 to 0.9): a hint of life with no visible wobble in lists.
 */
object TimeGoMotion {
    val expandEnter: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )
    val expandExit: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 1f,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )
    val fadeEnter: FiniteAnimationSpec<Float> = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
    val fadeExit: FiniteAnimationSpec<Float> = tween(durationMillis = 120, easing = FastOutLinearInEasing)

    /** Whole-content swaps and scale-ins (sheets, diagrams, chart sweeps). */
    val contentEnter: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    val contentExit: FiniteAnimationSpec<Float> = tween(durationMillis = 120, easing = FastOutLinearInEasing)

    val navigationInOffset: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
    val navigationOutOffset: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 1f,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
    val pulseWidth: FiniteAnimationSpec<Dp> = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)

    /** Small state changes: chevron turns, row emphasis. Light bounce, quick. */
    val rotate: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)

    /** Floating controls gliding to a new resting height (e.g. above the rest bar). */
    val liftSpec: FiniteAnimationSpec<Dp> = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)

    /** Colour cross-fades never spring (overshoot would flash the wrong colour). */
    val colorFade: FiniteAnimationSpec<androidx.compose.ui.graphics.Color> = tween(durationMillis = 200, easing = LinearOutSlowInEasing)

    /** A value settling to a new reading (progress rings, the rest-timer arc): a light overshoot, then still. */
    val settle: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
}
