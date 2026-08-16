package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lsing.timego.ui.theme.TimeGoMotion

/** Shared expand/collapse transition for exercise rows and library sections. */
@Composable
fun AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(TimeGoMotion.expandEnter) + fadeIn(TimeGoMotion.fadeEnter),
        exit = shrinkVertically(TimeGoMotion.expandExit) + fadeOut(TimeGoMotion.fadeExit),
    ) {
        Column { content() }
    }
}
