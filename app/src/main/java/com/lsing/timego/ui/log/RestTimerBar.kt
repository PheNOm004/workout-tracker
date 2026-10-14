package com.lsing.timego.ui.log

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.domain.RestTimerState
import com.lsing.timego.domain.formatRestClock
import com.lsing.timego.domain.remainingSeconds
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Pinned above the session list after a working set: countdown, progress, +/-15s and skip. Past zero
 *  it flips to "hit your next set" and counts the overrun, which doubles as time-since-last-set. */
@Composable
fun RestTimerBar(
    state: RestTimerState,
    onAdjust: (deltaSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by produceState(System.currentTimeMillis(), state) {
        while (true) {
            value = System.currentTimeMillis()
            delay(250)
        }
    }
    val remaining = state.remainingSeconds(now)
    val over = remaining <= 0
    val progress by animateFloatAsState(
        targetValue = (1f - remaining.toFloat() / state.totalSeconds).coerceIn(0f, 1f),
        label = "restProgress",
    )

    SurfaceCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        hero = true,
        cornerRadius = 10.dp,
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (over) "Rest over: hit your next set" else "Rest",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    )
                    Text(formatRestClock(remaining), style = LedgerFigureEmphasis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onAdjust(-15) }) { Text("-15s") }
                    OutlinedButton(onClick = { onAdjust(15) }) { Text("+15s") }
                    Button(onClick = onDismiss) { Text(if (over) "Done" else "Skip") }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
            )
        }
    }
}
