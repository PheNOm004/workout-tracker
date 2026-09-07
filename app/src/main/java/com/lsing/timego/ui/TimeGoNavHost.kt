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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex
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
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import com.lsing.timego.ui.common.tactilePress
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightEdgeHairline
import com.lsing.timego.ui.theme.NightSheenTop
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
        RetainedNavContent(
            selectedRoute = selectedRoute,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun RetainedNavContent(
    selectedRoute: String,
    modifier: Modifier = Modifier,
) {
    // Lazily mount tabs so initial app startup is instant (only LogScreen constructed initially),
    // and keep visited tabs alive in the composition tree to avoid cold reconstruction jank.
    var visitedRoutes by rememberSaveable { mutableStateOf(setOf("log")) }
    LaunchedEffect(selectedRoute) {
        if (!visitedRoutes.contains(selectedRoute)) {
            visitedRoutes = visitedRoutes + selectedRoute
        }
    }

    var activeRoute by remember { mutableStateOf(selectedRoute) }
    var previousRoute by remember { mutableStateOf<String?>(null) }
    var forward by remember { mutableStateOf(true) }

    val transitionProgress = remember { Animatable(1f) }

    LaunchedEffect(selectedRoute) {
        if (selectedRoute != activeRoute) {
            val fromIndex = destinations.indexOfRoute(activeRoute).coerceAtLeast(0)
            val toIndex = destinations.indexOfRoute(selectedRoute).coerceAtLeast(0)
            forward = toIndex >= fromIndex
            previousRoute = activeRoute
            activeRoute = selectedRoute
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = EaseInOut),
            )
            previousRoute = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        destinations.forEach { dest ->
            val route = dest.route
            if (visitedRoutes.contains(route)) {
                val isTarget = (route == activeRoute)
                val isPrevious = (route == previousRoute)
                val isAnimating = (previousRoute != null)

                val progress = transitionProgress.value
                val (alpha, offsetFraction) = when {
                    isTarget && isAnimating -> {
                        val startOffset = if (forward) 0.33f else -0.33f
                        Pair(progress, (1f - progress) * startOffset)
                    }
                    isPrevious && isAnimating -> {
                        val endOffset = if (forward) -0.33f else 0.33f
                        Pair(1f - progress, progress * endOffset)
                    }
                    isTarget -> Pair(1f, 0f)
                    else -> Pair(0f, 0f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isTarget) 1f else 0f)
                        .graphicsLayer {
                            this.alpha = alpha
                            this.translationX = offsetFraction * size.width
                        }
                        .then(
                            if (!isTarget) {
                                Modifier
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                    .clearAndSetSemantics { }
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    when (route) {
                        "log" -> com.lsing.timego.ui.log.LogScreen()
                        "progress" -> com.lsing.timego.ui.progress.ProgressScreen()
                        "routines" -> com.lsing.timego.ui.routines.RoutinesScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeGoBottomDock(
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
) {
    val selectedIndex = destinations.indexOfRoute(selectedRoute).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = NightDeckHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = NightEdgeHairline,
                    shape = RoundedCornerShape(22.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            0f to NightSheenTop,
                            0.35f to androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    val tabWidth = maxWidth / destinations.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * selectedIndex,
                        animationSpec = tween(durationMillis = 300, easing = EaseInOut),
                        label = "navIndicatorOffset",
                    )

                    // Sliding active highlight capsule behind selected tab
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .height(52.dp)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    )

                    // Sliding top precision notch on active tab
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .padding(top = 1.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        destinations.forEach { destination ->
                            val isSelected = selectedRoute == destination.route
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "navColor",
                            )
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.08f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                label = "navIconScale",
                            )

                            val interactionSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .tactilePress(interactionSource, pressedScale = 0.92f)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onSelectRoute(destination.route) },
                                    ),
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
                                        modifier = Modifier
                                            .size(22.dp)
                                            .scale(iconScale),
                                    )
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
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
    }
}

private fun List<TimeGoDestination>.indexOfRoute(route: String): Int = indexOfFirst { it.route == route }
