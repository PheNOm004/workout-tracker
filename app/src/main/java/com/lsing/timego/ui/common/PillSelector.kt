package com.lsing.timego.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.TimeGoMotion

/**
 * A pill with a sliding, translucent selection indicator (same construction as the bottom dock): equal-width
 * options, the selected one tinted with the action colour. Replaces Material's connected segmented buttons, whose
 * per-segment corner shapes clash with a fully rounded, frosted container.
 */
@Composable
fun PillSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(26.dp)
    val optionWidth = 96.dp
    Box(modifier = modifier.padding(5.dp)) {
        val indicatorX by animateDpAsState(
            targetValue = optionWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "pillSelectorOffset",
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorX.roundToPx(), 0) }
                .width(optionWidth)
                .height(46.dp)
                .clip(shape)
                .background(accent.copy(alpha = 0.2f))
                .border(1.dp, accent.copy(alpha = 0.4f), shape),
        )
        Row {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = TimeGoMotion.colorFade,
                    label = "pillSelectorColor",
                )
                val source = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .width(optionWidth)
                        .height(46.dp)
                        .clip(shape)
                        .tactilePress(source, pressedScale = 0.96f)
                        .clickable(interactionSource = source, indication = null) { onSelect(index) }
                        .semantics { role = Role.Tab; this.selected = selected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
    }
}
