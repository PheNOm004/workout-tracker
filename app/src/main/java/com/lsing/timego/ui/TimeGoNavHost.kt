package com.lsing.timego.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.ui.theme.TimeGoMotion

private data class TimeGoDestination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    TimeGoDestination("log", "Log", Icons.Filled.FitnessCenter),
    TimeGoDestination("progress", "Progress", Icons.AutoMirrored.Filled.ShowChart),
    TimeGoDestination("routines", "Routines", Icons.AutoMirrored.Filled.List),
)

/**
 * Deliberately not `NavHost` for these three root tabs: the standard bottom-nav pattern
 * (`popUpTo` + `saveState`/`restoreState`) hits a real Navigation-Compose limitation where
 * restore-path navigations skip `enterTransition`/`exitTransition`/`pop*Transition` entirely, no
 * matter how they're configured -- confirmed by testing with all four explicitly set and still
 * getting an instant cut. Driving the switch with our own [AnimatedContent] sidesteps that path
 * completely. None of Log/Progress/Routines push further destinations today, so there's no
 * back-stack to lose; [BackHandler] below restores the one piece of back-button behavior a real
 * NavController would have given for free (back returns to Log rather than exiting).
 */
@Composable
fun TimeGoNavHost() {
    var selectedRoute by rememberSaveable { mutableStateOf("log") }
    BackHandler(enabled = selectedRoute != "log") { selectedRoute = "log" }

    Scaffold(
        bottomBar = {
            TimeGoBottomDock(
                selectedRoute = selectedRoute,
                onSelectRoute = { selectedRoute = it },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedRoute,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                // Slide direction follows tab order (Log -> Progress -> Routines) so switching
                // feels spatial -- moving right through the row slides left-to-right, and back
                // reverses it -- rather than every switch sliding the same way regardless of
                // which tab you came from.
                val forward = destinations.indexOfRoute(targetState) >= destinations.indexOfRoute(initialState)
                val enter = slideInHorizontally(TimeGoMotion.navigationInOffset) { width -> if (forward) width / 3 else -width / 3 } +
                    fadeIn(TimeGoMotion.contentEnter)
                val exit = slideOutHorizontally(TimeGoMotion.navigationOutOffset) { width -> if (forward) -width / 3 else width / 3 } +
                    fadeOut(TimeGoMotion.contentExit)
                enter togetherWith exit
            },
            label = "tabContent",
        ) { route ->
            when (route) {
                "log" -> com.lsing.timego.ui.log.LogScreen()
                "progress" -> com.lsing.timego.ui.progress.ProgressScreen()
                "routines" -> com.lsing.timego.ui.routines.RoutinesScreen()
            }
        }
    }
}

@Composable
private fun TimeGoBottomDock(
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(32.dp),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val isSelected = selectedRoute == destination.route
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "navScale",
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        label = "navBg",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navColor",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(containerColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectRoute(destination.route) },
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                                ),
                                color = contentColor,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun List<TimeGoDestination>.indexOfRoute(route: String): Int = indexOfFirst { it.route == route }
