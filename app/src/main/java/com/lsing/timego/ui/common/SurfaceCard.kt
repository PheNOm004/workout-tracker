package com.lsing.timego.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightDeckLow
import com.lsing.timego.ui.theme.NightEdgeHairline
import com.lsing.timego.ui.theme.NightGlow
import com.lsing.timego.ui.theme.NightSheenTop

/**
 * Backlit's reusable raised-surface primitive. It owns the tonal deck and light edge so later
 * screen batches do not reimplement depth cues; glow remains opt-in and budgeted by the caller.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    glow: Boolean = false,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val deck = if (hero) NightDeckHigh else NightDeckLow

    // clip(shape) stays outermost so a caller-supplied .clickable's ripple (e.g. the Log
    // landing card) is still bounded to the rounded shape. background() previously had no
    // shape argument, so it drew a plain sharp-cornered rectangle -- fine when nothing sat
    // between clip and background, but a caller modifier that adds its own padding (e.g.
    // StatTile's spacing between tiles) shrinks the box before background runs, and that
    // inset sharp rectangle's corners poked past the outer clip's rounded boundary. Passing
    // shape here makes background round itself at whatever size it actually draws at,
    // matching border below, regardless of what the caller's modifier does in between.
    Box(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .background(deck, shape)
            .drawBehind {
                if (hero) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to NightSheenTop,
                            0.35f to Color.Transparent,
                        ),
                    )
                }
                if (glow) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(NightGlow, Color.Transparent),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.maxDimension * 0.85f,
                        ),
                    )
                }
            }
            .border(1.dp, NightEdgeHairline, shape),
        content = content,
    )
}
