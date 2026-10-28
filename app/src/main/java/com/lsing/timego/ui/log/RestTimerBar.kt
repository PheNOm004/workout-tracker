package com.lsing.timego.ui.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.domain.RestTimerState
import com.lsing.timego.domain.formatRestClock
import com.lsing.timego.domain.remainingSeconds
import com.lsing.timego.ui.common.frosted
import com.lsing.timego.ui.common.tactilePress
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.PlateBlue
import com.lsing.timego.ui.theme.PlateYellow
import com.lsing.timego.ui.theme.TimeGoMotion
import kotlinx.coroutines.delay

/** Whole seconds left, negative once over. Emits only when the second changes, so readers recompose once per
 *  second rather than on every poll tick. */
@Composable
private fun rememberRestRemaining(state: RestTimerState): State<Int> =
    produceState(state.remainingSeconds(System.currentTimeMillis()), state) {
        while (true) {
            value = state.remainingSeconds(System.currentTimeMillis())
            delay(250)
        }
    }

/** The countdown ring with the clock inside: drains as rest elapses, fills amber once over. */
@Composable
private fun RestRing(remaining: Int, totalSeconds: Int, size: Dp, fontSize: Int, modifier: Modifier = Modifier, strokeWidth: Dp = size / 11.6f) {
    val over = remaining <= 0
    val remainingFraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / totalSeconds).coerceIn(0f, 1f),
        animationSpec = TimeGoMotion.settle,
        label = "restRemaining",
    )
    val ringColor = if (over) PlateYellow else PlateBlue
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            drawArc(ringColor, -90f, if (over) 360f else 360f * remainingFraction, false, Offset(inset, inset), arcSize, style = stroke)
        }
        Text(
            formatRestClock(remaining),
            style = LedgerFigureEmphasis.copy(fontSize = fontSize.sp),
            color = if (over) PlateYellow else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Floating rest timer, docked above the bottom bar so it sits under the thumb and overlays the list
 * instead of pushing it. Collapsed it is a ring with the countdown, a label and Skip; tapping the bar
 * expands a second row with large -15s / +15s buttons. Past zero the ring fills amber and it reads
 * "Rest over", counting the overrun, which doubles as time-since-last-set.
 *
 * [onMinimize], when set, adds a button that shrinks the bar back to the [RestTimerChip].
 */
@Composable
fun RestTimerBar(
    state: RestTimerState,
    onAdjust: (deltaSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimize: (() -> Unit)? = null,
) {
    val remaining by rememberRestRemaining(state)
    val over = remaining <= 0
    var expanded by rememberSaveable { mutableStateOf(false) }
    val toggleSource = remember { MutableInteractionSource() }
    val clock = formatRestClock(remaining)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .frosted(RoundedCornerShape(30.dp), borderWidth = 0.5.dp)
            .animateContentSize(TimeGoMotion.expandEnter)
            .tactilePress(toggleSource, pressedScale = 0.985f)
            .clickable(
                interactionSource = toggleSource,
                indication = null,
                onClickLabel = if (expanded) "Hide time adjustments" else "Adjust rest time",
            ) { expanded = !expanded }
            .padding(12.dp)
            .semantics { contentDescription = if (over) "Rest over, $clock" else "Resting, $clock left" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RestRing(remaining, state.totalSeconds, size = 58.dp, fontSize = 14)
            Column(modifier = Modifier.weight(1f)) {
                Text(if (over) "Rest over" else "Rest", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (over) "Hit your next set" else if (expanded) "Tap to collapse" else "Tap to adjust",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onMinimize != null) {
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimise rest timer")
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.height(44.dp)) { Text(if (over) "Done" else "Skip") }
        }
        AnimatedVisibility(visible = expanded) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { onAdjust(-15) }, modifier = Modifier.weight(1f).height(44.dp)) { Text("-15s") }
                FilledTonalButton(onClick = { onAdjust(15) }, modifier = Modifier.weight(1f).height(44.dp)) { Text("+15s") }
            }
        }
    }
}

/** The rest timer shrunk to a frosted circle (ring + clock) for when you are away from the Log tab. Tap to expand. */
@Composable
fun RestTimerChip(state: RestTimerState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val remaining by rememberRestRemaining(state)
    val source = remember { MutableInteractionSource() }
    val clock = formatRestClock(remaining)
    Box(
        modifier = modifier
            .size(72.dp)
            .padding(8.dp)
            .frosted(CircleShape, borderWidth = 0.5.dp)
            .tactilePress(source, pressedScale = 0.92f)
            .clickable(interactionSource = source, indication = null, onClickLabel = "Show rest timer", onClick = onClick)
            .semantics { contentDescription = if (remaining <= 0) "Rest over, $clock" else "Resting, $clock left" },
        contentAlignment = Alignment.Center,
    ) {
        RestRing(remaining, state.totalSeconds, size = 50.dp, fontSize = 11, strokeWidth = 3.dp)
    }
}
