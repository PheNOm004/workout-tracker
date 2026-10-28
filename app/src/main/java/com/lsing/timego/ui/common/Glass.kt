package com.lsing.timego.ui.common

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.LocalTimeGoPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** The one [HazeState] for the app's content layer. Frosted surfaces blur whatever is marked as a source
 *  in this state; null (previews, tests) makes [frosted] fall back to a plain translucent fill. */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/** Height of the floating bottom dock. Scrollable content adds this as bottom padding so its last item
 *  can always scroll clear of the dock, while the rest of the list passes underneath it. */
val LocalDockHeight = compositionLocalOf { 0.dp }

/**
 * Frosted glass: blurs the content behind it and tints it with the palette's glass colour. Reserved for
 * the few transient, floating surfaces (bottom dock, rest timer). Below Android 12 there is no render-
 * effect blur, so Haze draws only the tint, which is still a translucent, legible surface.
 */
@Composable
fun Modifier.frosted(shape: Shape, blurRadius: Dp = 24.dp): Modifier {
    val palette = LocalTimeGoPalette.current
    val haze = LocalHazeState.current
    val base = clip(shape)
    return (if (haze == null) base else base.hazeEffect(haze) {
        this.blurRadius = blurRadius
        tints = listOf(HazeTint(palette.glass))
        noiseFactor = 0f
    }).border(1.dp, palette.glassEdge, shape)
}
