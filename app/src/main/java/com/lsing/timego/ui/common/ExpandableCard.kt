package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.Spacing
import com.lsing.timego.ui.theme.TimeGoMotion

/** Chevron that rotates a quarter turn when [expanded]; shared by every expandable list row so the Log
 *  exercise list and the program lists animate identically. */
@Composable
fun ExpandChevron(expanded: Boolean, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = TimeGoMotion.rotate,
        label = "chevronRotation",
    )
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = if (expanded) "Collapse" else "Expand",
        modifier = modifier.graphicsLayer { rotationZ = rotation },
    )
}

/** The Log page's exercise-row look as a reusable shell: a console-plate card that lifts to the hero
 *  surface and grows to reveal [content] when [expanded]. */
@Composable
fun ExpandableCard(
    expanded: Boolean,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SurfaceCard(
        hero = expanded,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = TimeGoMotion.expandEnter)
            .padding(vertical = 4.dp),
    ) {
        Column {
            header()
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(TimeGoMotion.expandEnter) + fadeIn(TimeGoMotion.fadeEnter),
                exit = shrinkVertically(TimeGoMotion.expandExit) + fadeOut(TimeGoMotion.fadeExit),
            ) {
                Column { content() }
            }
        }
    }
}

/** One-line header for [ExpandableCard]: [title], an optional right-aligned [summary], then the chevron. */
@Composable
fun ExpandableHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    summary: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(Spacing.Medium),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (summary != null) {
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.Small),
            )
        }
        ExpandChevron(expanded)
    }
}
