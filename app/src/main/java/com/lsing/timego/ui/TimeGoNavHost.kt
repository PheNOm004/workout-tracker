package com.lsing.timego.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lsing.timego.ui.theme.TimeGoMotion

private data class TimeGoDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

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
            NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedRoute == destination.route,
                        onClick = { selectedRoute = destination.route },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
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

private fun List<TimeGoDestination>.indexOfRoute(route: String): Int = indexOfFirst { it.route == route }
