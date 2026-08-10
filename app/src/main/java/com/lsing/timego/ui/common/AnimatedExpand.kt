package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared expand/collapse transition for the app's several collapsible sections (exercise log
 *  rows, library category/muscle-group headers) -- spring-based rather than the prior linear
 *  tween, per the Training Ledger direction's motion answer to "feels generic/flat": a page
 *  entry settling into place has weight, not a mechanical ease curve. Exit stays snappier than
 *  enter (higher stiffness, no bounce) so collapsing never feels like it's fighting the user.
 *  Wraps [content] in a Column since AnimatedVisibility needs a single child layout slot. */
@Composable
fun AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        ) + fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = shrinkVertically(
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh)),
    ) {
        Column { content() }
    }
}
