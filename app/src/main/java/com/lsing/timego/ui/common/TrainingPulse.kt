package com.lsing.timego.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.TimeGoMotion
import com.lsing.timego.ui.theme.Spacing

/** A quiet coral edge that marks the one exercise currently receiving attention. */
@Composable
fun TrainingPulse(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pulseWidth by animateDpAsState(
        targetValue = if (active) 3.dp else 0.dp,
        animationSpec = TimeGoMotion.pulseWidth,
        label = "training pulse width",
    )
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(pulseWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        if (active) Spacer(Modifier.width(Spacing.ExtraSmall))
        content()
    }
}
