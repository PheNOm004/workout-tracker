package com.lsing.timego.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * Adds an immediate tactile press-depth spring animation to any clickable surface.
 * When pressed down, scales smoothly to [pressedScale]; on release, springs back
 * with responsive physics matching the Engine-Room Console feel.
 */
fun Modifier.tactilePress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "tactilePressScale",
    )
    this.scale(scale)
}
