package com.lsing.timego.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.TimeGoMotion
import kotlinx.coroutines.delay

/** A quiet coral edge that marks the active exercise and briefly expands when a set is saved. */
@Composable
fun TrainingPulse(
    active: Boolean,
    modifier: Modifier = Modifier,
    pulseId: Long = 0L,
    content: @Composable () -> Unit,
) {
    var isBursting by remember { mutableStateOf(false) }
    LaunchedEffect(pulseId) {
        if (pulseId > 0L) {
            isBursting = true
            delay(180)
            isBursting = false
        }
    }
    val pulseWidth by animateDpAsState(
        targetValue = if (isBursting) 6.dp else if (active) 3.dp else 0.dp,
        animationSpec = TimeGoMotion.pulseWidth,
        label = "training pulse width",
    )
    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(pulseWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
