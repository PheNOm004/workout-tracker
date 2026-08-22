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
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val deck = if (hero) NightDeckHigh else NightDeckLow

    Box(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .background(deck)
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
