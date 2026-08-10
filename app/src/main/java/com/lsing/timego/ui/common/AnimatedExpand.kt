package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared expand/collapse transition for the app's several collapsible sections (exercise log
 *  rows, library category/muscle-group headers) -- ease-out entering over 250ms, faster ease-in
 *  exiting over 150ms, per standard "exit-faster-than-enter" motion convention. Wraps [content]
 *  in a Column since AnimatedVisibility needs a single child layout slot, matching what each
 *  call site's previous bare `if (expanded) { ... }` block implicitly required anyway. */
@Composable
fun AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(tween(250, easing = LinearOutSlowInEasing)) + fadeIn(tween(250)),
        exit = shrinkVertically(tween(150, easing = FastOutLinearInEasing)) + fadeOut(tween(150)),
    ) {
        Column { content() }
    }
}
