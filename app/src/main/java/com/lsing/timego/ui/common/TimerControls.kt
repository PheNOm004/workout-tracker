package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lsing.timego.domain.HoldTimerPhase
import com.lsing.timego.domain.timerPhaseAt
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Poll cadence, not tick size. The displayed value is recomputed from the wall clock every pass
 *  (see [timerPhaseAt]), so this only controls how promptly a second-boundary crossing shows up;
 *  polling faster than 1s keeps the display responsive without the value itself drifting, and
 *  identical phases are structurally equal so an unchanged second costs no recomposition. */
private const val POLL_INTERVAL_MILLIS = 250L

/** The start / counting-down / running control strip shared by the cardio and hold logging rows.
 *  Both previously carried their own near-identical copy of this state machine, which meant the
 *  same timer bug had to be fixed twice. [formatElapsed] is the only genuine difference between
 *  them -- cardio shows mm:ss, holds show raw seconds.
 *
 *  Owns the timer's own state; [onStop] receives the elapsed seconds and the strip resets itself
 *  to idle afterwards. */
@Composable
fun TimerControls(
    delaySeconds: Int,
    formatElapsed: (Int) -> String,
    onEnterManually: () -> Unit,
    onStop: (elapsedSeconds: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var startedAtEpochMillis by remember { mutableStateOf<Long?>(null) }
    var phase by remember { mutableStateOf<HoldTimerPhase?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(startedAtEpochMillis, delaySeconds, lifecycleOwner) {
        val startedAt = startedAtEpochMillis
        if (startedAt == null) {
            phase = null
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                phase = timerPhaseAt(startedAt, delaySeconds, System.currentTimeMillis())
                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                val enter = fadeIn() + slideInHorizontally { it / 4 }
                val exit = fadeOut() + slideOutHorizontally { -it / 4 }
                enter togetherWith exit
            },
            label = "timerPhaseTransition",
        ) { current ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (current) {
                    null -> {
                        Button(onClick = {
                            val now = System.currentTimeMillis()
                            phase = timerPhaseAt(now, delaySeconds, now)
                            startedAtEpochMillis = now
                        }) { Text("Start timer") }
                        TextButton(onClick = onEnterManually) { Text("Enter manually") }
                    }
                    is HoldTimerPhase.CountingDown -> {
                        Text(
                            "Starting in ${current.secondsRemaining}s...",
                            style = LedgerFigureValue,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { startedAtEpochMillis = null }) { Text("Cancel") }
                    }
                    is HoldTimerPhase.Running -> {
                        Text(
                            formatElapsed(current.elapsedSeconds),
                            style = LedgerFigureValue,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = {
                            onStop(current.elapsedSeconds)
                            startedAtEpochMillis = null
                        }) { Text("Stop & Log") }
                    }
                }
            }
        }
    }
}
