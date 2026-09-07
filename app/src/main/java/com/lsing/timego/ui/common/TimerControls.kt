package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.lsing.timego.ui.theme.TimeGoMotion
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

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightEdgeHairline
import com.lsing.timego.ui.theme.NightMint

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
 *  Styled in Night Training Console mint green, featuring an active digital stopwatch bezel.
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

    val isActive = phase != null
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "beaconAlpha",
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NightDeckHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            .border(
                width = 1.dp,
                color = if (isActive) NightMint.copy(alpha = 0.5f) else NightEdgeHairline,
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                val enter = fadeIn(TimeGoMotion.fadeEnter) + slideInHorizontally(TimeGoMotion.navigationInOffset) { it / 4 }
                val exit = fadeOut(TimeGoMotion.fadeExit) + slideOutHorizontally(TimeGoMotion.navigationOutOffset) { -it / 4 }
                enter togetherWith exit
            },
            label = "timerPhaseTransition",
            modifier = Modifier.padding(Spacing.Medium),
        ) { current ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (current) {
                    null -> {
                        val startInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            interactionSource = startInteractionSource,
                            modifier = Modifier.tactilePress(startInteractionSource, pressedScale = 0.95f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NightMint,
                                contentColor = Color(0xFF0A1810),
                            ),
                            onClick = {
                                val now = System.currentTimeMillis()
                                phase = timerPhaseAt(now, delaySeconds, now)
                                startedAtEpochMillis = now
                            },
                        ) {
                            Text("Start timer", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        TextButton(onClick = onEnterManually) {
                            Text("Enter manually", color = NightMint)
                        }
                    }
                    is HoldTimerPhase.CountingDown -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NightMint.copy(alpha = beaconAlpha)),
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GET READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = NightMint.copy(alpha = 0.75f),
                            )
                            Text(
                                "Starting in ${current.secondsRemaining}s...",
                                style = LedgerFigureValue.copy(fontSize = 18.sp, color = NightMint),
                            )
                        }
                        val cancelInteractionSource = remember { MutableInteractionSource() }
                        OutlinedButton(
                            interactionSource = cancelInteractionSource,
                            modifier = Modifier.tactilePress(cancelInteractionSource, pressedScale = 0.95f),
                            onClick = { startedAtEpochMillis = null },
                        ) {
                            Text("Cancel")
                        }
                    }
                    is HoldTimerPhase.Running -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NightMint.copy(alpha = beaconAlpha)),
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "HOLDING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = NightMint.copy(alpha = 0.75f),
                            )
                            Text(
                                formatElapsed(current.elapsedSeconds),
                                style = LedgerFigureValue.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NightMint,
                                ),
                            )
                        }
                        val stopInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            interactionSource = stopInteractionSource,
                            modifier = Modifier.tactilePress(stopInteractionSource, pressedScale = 0.95f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NightMint,
                                contentColor = Color(0xFF0A1810),
                            ),
                            onClick = {
                                onStop(current.elapsedSeconds)
                                startedAtEpochMillis = null
                            },
                        ) {
                            Text("Stop & Log", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
