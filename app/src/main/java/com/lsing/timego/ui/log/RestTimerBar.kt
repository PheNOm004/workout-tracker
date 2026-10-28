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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Floating rest timer, docked above the bottom bar so it sits under the thumb and overlays the list
 * instead of pushing it. Collapsed it is a ring with the countdown, a label and Skip; tapping the bar
 * expands a second row with large -15s / +15s buttons. Past zero the ring fills amber and it reads
 * "Rest over", counting the overrun, which doubles as time-since-last-set.
 */
@Composable
fun RestTimerBar(
    state: RestTimerState,
    onAdjust: (deltaSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Emits whole seconds only, so the bar recomposes once per second rather than on every poll tick.
    val remaining by produceState(state.remainingSeconds(System.currentTimeMillis()), state) {
        while (true) {
            value = state.remainingSeconds(System.currentTimeMillis())
            delay(250)
        }
    }
    val over = remaining <= 0
    var expanded by rememberSaveable { mutableStateOf(false) }
    val remainingFraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / state.totalSeconds).coerceIn(0f, 1f),
        animationSpec = TimeGoMotion.settle,
        label = "restRemaining",
    )
    val toggleSource = remember { MutableInteractionSource() }
    val ringColor = if (over) PlateYellow else PlateBlue
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val clock = formatRestClock(remaining)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .frosted(RoundedCornerShape(30.dp))
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
            Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(58.dp)) {
                    val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    val inset = stroke.width / 2f
                    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                    drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
                    drawArc(ringColor, -90f, if (over) 360f else 360f * remainingFraction, false, Offset(inset, inset), arcSize, style = stroke)
                }
                Text(
                    clock,
                    style = LedgerFigureEmphasis.copy(fontSize = 14.sp),
                    color = if (over) PlateYellow else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(if (over) "Rest over" else "Rest", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (over) "Hit your next set" else if (expanded) "Tap to collapse" else "Tap to adjust",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
