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
import androidx.compose.ui.draw.shadow
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
import com.lsing.timego.ui.theme.RivetHighlight
import com.lsing.timego.ui.theme.RivetShadow

/**
 * The Gauge Panel's reusable raised-surface primitive: a riveted steel plate, not a flat tonal
 * card. Depth comes from a real cast shadow plus a brass-lit top bezel; [riveted] adds the four
 * corner fasteners for panels that should read as mounted hardware (hero cards, the gauge dial's
 * housing) rather than every card app-wide, which would turn the signature into wallpaper.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    glow: Boolean = false,
    riveted: Boolean = false,
    cornerRadius: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val deck = if (hero) NightDeckHigh else NightDeckLow
    val elevation = if (hero) 6.dp else 2.dp

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
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .then(modifier)
            .background(deck, shape)
            .drawBehind {
                // Brass-lit top bezel: a real light source raking the top edge of the plate,
                // not a flat hairline -- this is the primitive-level fix for the "flat" read,
                // since every SurfaceCard call site inherits it without touching each screen.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to NightSheenTop,
                        0.4f to Color.Transparent,
                    ),
                )
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
                if (riveted) {
                    val inset = 10.dp.toPx()
                    val r = 2.2.dp.toPx()
                    val corners = listOf(
                        Offset(inset, inset),
                        Offset(size.width - inset, inset),
                        Offset(inset, size.height - inset),
                        Offset(size.width - inset, size.height - inset),
                    )
                    corners.forEach { center ->
                        drawCircle(RivetShadow, radius = r, center = center + Offset(0.6f, 0.9f))
                        drawCircle(Color(0xFF3A4247), radius = r, center = center)
                        drawCircle(RivetHighlight, radius = r * 0.4f, center = center - Offset(0.5f, 0.5f))
                    }
                }
            }
            .border(1.dp, NightEdgeHairline, shape),
        content = content,
    )
}
