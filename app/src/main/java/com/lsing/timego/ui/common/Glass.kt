package com.lsing.timego.ui.common

import androidx.compose.material3.MaterialTheme
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

/** Opacity of sheets and dialogs. They are separate windows, so the in-app backdrop blur cannot reach them and a
 *  window-level blur is not reliable; at low opacity the screen behind shows through sharp and fights the text.
 *  So they are near-opaque, with just enough transparency to feel layered. */
const val WindowSurfaceAlpha = 0.94f

/** Blur radius shared by in-app frosted surfaces and the window blur behind sheets and dialogs. */
val GlassBlurRadius = 11.dp

/** The one [HazeState] for the app's content layer. Frosted surfaces blur whatever is marked as a source
 *  in this state; null (previews, tests) makes [frosted] fall back to a plain translucent fill. */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/** Height of the floating bottom dock. Scrollable content adds this as bottom padding so its last item
 *  can always scroll clear of the dock, while the rest of the list passes underneath it. */
val LocalDockHeight = compositionLocalOf { 0.dp }

/** Measured height of the full rest-timer bar above the dock (zero when it is hidden or only a chip). Scrolling
 *  content and floating controls (Progress's selector) add it so they ride above the bar. */
val LocalRestClearance = compositionLocalOf { 0.dp }

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
fun Modifier.frosted(shape: Shape, blurRadius: Dp = GlassBlurRadius, borderWidth: Dp = 1.dp): Modifier {
    val palette = LocalTimeGoPalette.current
    val haze = LocalHazeState.current
    // The blur samples content with transparent gaps; an opaque base under it hides the crisp content beneath the
    // glass, otherwise text shows through the frost sharp.
    val baseColor = MaterialTheme.colorScheme.background
    val base = clip(shape)
    return (if (haze == null) base else base.hazeEffect(haze) {
        this.blurRadius = blurRadius
        tints = listOf(HazeTint(palette.glass))
        noiseFactor = 0f
        backgroundColor = baseColor
    }).border(borderWidth, palette.glassEdge, shape)
}
