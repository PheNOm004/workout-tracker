package com.lsing.timego.ui.common

import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.BoxScope
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

/** Bottom space the full rest-timer bar currently occupies above the dock (zero when it is hidden or a chip). */
val LocalRestClearance = compositionLocalOf { 0.dp }

/** True while the rest bar is expanded over a non-Log tab; floating controls (e.g. Progress's segment selector)
 *  hide while it covers their spot. */
val LocalRestBarCovering = compositionLocalOf { false }

/** Holds the one floating overlay a screen has asked the app shell to draw (see [ScreenOverlay]). */
class ScreenOverlayHost {
    var content by mutableStateOf<(@Composable BoxScope.() -> Unit)?>(null)
}

val LocalScreenOverlayHost = compositionLocalOf<ScreenOverlayHost?> { null }

/**
 * Floats [content] above the screen by drawing it in the app shell, as a sibling of the blur source rather than
 * inside it. A frosted surface cannot blur a source that contains it, so any screen-owned floating control that
 * wants glass (like Progress's segment selector) goes through here. [content] aligns itself within the shell's
 * full-screen box (e.g. `Modifier.align(Alignment.BottomCenter)`).
 */
@Composable
fun ScreenOverlay(content: @Composable BoxScope.() -> Unit) {
    val host = LocalScreenOverlayHost.current ?: return
    val latest by rememberUpdatedState(content)
    DisposableEffect(host) {
        host.content = { latest() }
        onDispose { host.content = null }
    }
}

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
