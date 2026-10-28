package com.lsing.timego.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.CardFill
import com.lsing.timego.ui.theme.Hairline
import com.lsing.timego.ui.theme.RaisedFill

/**
 * The app's reusable card: a soft tonal plate with a hairline edge. Depth is a tonal step ([hero] raises
 * it one level) rather than shadows or gradients, so it costs nothing to draw, stays crisp in both light
 * and dark, and scrolls without overdraw.
 *
 * clip(shape) stays outermost so a caller-supplied .clickable ripple is bounded to the rounded shape, and
 * background/border take the shape explicitly so they round at whatever size the caller's modifier leaves.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    cornerRadius: Dp = 24.dp,
    /** Overrides the fill, e.g. with the glass tint for dialogs. */
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .background(containerColor ?: if (hero) RaisedFill else CardFill, shape)
            .border(1.dp, Hairline, shape),
        content = content,
    )
}
