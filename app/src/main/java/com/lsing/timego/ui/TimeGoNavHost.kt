package com.lsing.timego.ui

import com.lsing.timego.ui.common.ScreenOverlayHost
import com.lsing.timego.ui.common.LocalScreenOverlayHost
import com.lsing.timego.ui.common.LocalRestClearance
import com.lsing.timego.ui.log.RestTimerChip
import com.lsing.timego.ui.log.RestTimerBar
import com.lsing.timego.ui.log.LogViewModel
import com.lsing.timego.domain.RestTimerState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.SizeTransform
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.lsing.timego.ui.common.LocalDockHeight
import com.lsing.timego.ui.common.LocalHazeState
import com.lsing.timego.ui.common.frosted
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import com.lsing.timego.ui.common.tactilePress
import com.lsing.timego.ui.theme.TimeGoMotion
import com.lsing.timego.profile.ProfileRepository
import com.lsing.timego.profile.TrainingProfile
import com.lsing.timego.ui.onboarding.OnboardingScreen
import com.lsing.timego.ui.onboarding.OnboardingViewModel

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
private const val EXIT_BACK_WINDOW_MILLIS = 2_000L

@Composable
fun TimeGoNavHost() {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context.applicationContext) }
    // null until the saved profile has actually loaded. Defaulting to an empty TrainingProfile here made
    // every launch look like a first run, flashing the questions page until DataStore answered.
    val loadedProfile by profileRepository.profile.collectAsStateWithLifecycle<TrainingProfile?>(initialValue = null)
    var onboardingDismissedForSession by rememberSaveable { mutableStateOf(false) }
    val profile = loadedProfile
    if (profile == null) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        )
        return
    }
    if (
        profile.onboardingVersion < ProfileRepository.CURRENT_ONBOARDING_VERSION &&
        !profile.invitationDismissed &&
        !onboardingDismissedForSession
    ) {
        val onboardingViewModel: OnboardingViewModel = viewModel(key = "root_onboarding")
        OnboardingScreen(
            viewModel = onboardingViewModel,
            onFinished = { onboardingDismissedForSession = true },
        )
        return
    }

    var selectedRoute by rememberSaveable { mutableStateOf("log") }
    BackHandler(enabled = selectedRoute != "log") { selectedRoute = "log" }
    // Registered before any screen's handler, so a deeper layer (e.g. Log's picker/session) always wins;
    // this only fires once the user is at the Log landing page. Two backs within 2s exit, like games.
    var lastExitBackAtMillis by remember { mutableStateOf(0L) }
    BackHandler(enabled = selectedRoute == "log") {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastExitBackAtMillis <= EXIT_BACK_WINDOW_MILLIS) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastExitBackAtMillis = now
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Content runs under the floating dock so its glass has something to blur; the dock height is shared
    // so each scrolling screen can pad its last item clear of it.
    val hazeState = rememberHazeState()
    var dockHeightPx by remember { mutableIntStateOf(0) }
    val dockHeight = with(LocalDensity.current) { dockHeightPx.toDp() }
    // The rest timer belongs to the whole app, not the Log tab: full bar on Log, a corner circle elsewhere.
    val logViewModel: LogViewModel = viewModel()
    val restTimer by logViewModel.restTimer.collectAsStateWithLifecycle()
    var restExpandedOffLog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedRoute, restTimer == null) { restExpandedOffLog = false }
    val restBarShowing = restTimer != null && (selectedRoute == "log" || restExpandedOffLog)
    val overlayHost = remember { ScreenOverlayHost() }
    var restBarHeightPx by remember { mutableIntStateOf(0) }
    val restBarHeight = with(LocalDensity.current) { restBarHeightPx.toDp() }
    Scaffold(contentWindowInsets = WindowInsets.statusBars) { innerPadding ->
        CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalDockHeight provides dockHeight,
            LocalRestClearance provides if (restBarShowing) restBarHeight else 0.dp,
            LocalScreenOverlayHost provides overlayHost,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                RetainedNavContent(
                    selectedRoute = selectedRoute,
                    modifier = Modifier.padding(innerPadding).hazeSource(hazeState),
                )
                overlayHost.content?.let { overlay -> Box(modifier = Modifier.fillMaxSize()) { overlay() } }
                RestTimerOverlay(
                    timer = restTimer,
                    onLogTab = selectedRoute == "log",
                    expandedOffLog = restExpandedOffLog,
                    onExpandedOffLogChange = { restExpandedOffLog = it },
                    onAdjust = logViewModel::adjustRest,
                    onDismiss = {
                        // Skipping or finishing a rest means you are about to log: take them to the logging page.
                        logViewModel.dismissRest()
                        selectedRoute = "log"
                    },
                    onBarHeightChange = { restBarHeightPx = it },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = dockHeight),
                )
                TimeGoBottomDock(
                    selectedRoute = selectedRoute,
                    onSelectRoute = { selectedRoute = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { dockHeightPx = it.height },
                )
            }
        }
    }
}

private enum class RestOverlayMode { Hidden, Chip, Bar }

/** Hosts the rest timer above the dock: the full bar on the Log tab (or when expanded), a circle at the lower
 *  left elsewhere. Morphs between the two with a spring scale from the bottom-left corner. */
@Composable
private fun RestTimerOverlay(
    timer: RestTimerState?,
    onLogTab: Boolean,
    expandedOffLog: Boolean,
    onExpandedOffLogChange: (Boolean) -> Unit,
    onAdjust: (Int) -> Unit,
    onDismiss: () -> Unit,
    onBarHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the last timer so the exit animation still has something to draw after the state clears.
    var lastTimer by remember { mutableStateOf<RestTimerState?>(null) }
    if (timer != null) lastTimer = timer
    val mode = when {
        timer == null -> RestOverlayMode.Hidden
        onLogTab || expandedOffLog -> RestOverlayMode.Bar
        else -> RestOverlayMode.Chip
    }
    val origin = TransformOrigin(0f, 1f)
    LaunchedEffect(mode) { if (mode != RestOverlayMode.Bar) onBarHeightChange(0) }
    AnimatedContent(
        targetState = mode,
        modifier = modifier,
        contentAlignment = Alignment.BottomStart,
        transitionSpec = {
            (fadeIn(TimeGoMotion.fadeEnter) + scaleIn(TimeGoMotion.contentEnter, initialScale = 0.7f, transformOrigin = origin)) togetherWith
                (fadeOut(TimeGoMotion.fadeExit) + scaleOut(TimeGoMotion.contentExit, targetScale = 0.7f, transformOrigin = origin)) using
                SizeTransform(clip = false)
        },
        label = "restOverlay",
    ) { current ->
        val shown = lastTimer
        when {
            current == RestOverlayMode.Hidden || shown == null -> Spacer(Modifier.size(0.dp))
            current == RestOverlayMode.Chip -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                RestTimerChip(
                    state = shown,
                    onClick = { onExpandedOffLogChange(true) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            else -> RestTimerBar(
                state = shown,
                onAdjust = onAdjust,
                onDismiss = onDismiss,
                onMinimize = if (onLogTab) null else ({ onExpandedOffLogChange(false) }),
                modifier = Modifier.onSizeChanged { onBarHeightChange(it.height) },
            )
        }
    }
}

@Composable
private fun RetainedNavContent(
    selectedRoute: String,
    modifier: Modifier = Modifier,
) {
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = selectedRoute,
        transitionSpec = {
            val movingForward = destinations.indexOfRoute(targetState) >= destinations.indexOfRoute(initialState)
            val enter = slideInHorizontally(TimeGoMotion.navigationInOffset) { width -> if (movingForward) width / 5 else -width / 5 } +
                fadeIn(TimeGoMotion.contentEnter)
            val exit = slideOutHorizontally(TimeGoMotion.navigationOutOffset) { width -> if (movingForward) -width / 5 else width / 5 } +
                fadeOut(TimeGoMotion.contentExit)
            enter togetherWith exit
        },
        contentKey = { it },
        label = "rootDestinationTransition",
        modifier = modifier.fillMaxSize(),
    ) { route ->
        stateHolder.SaveableStateProvider(route) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (route) {
                    "log" -> com.lsing.timego.ui.log.LogScreen()
                    "progress" -> com.lsing.timego.ui.progress.ProgressScreen()
                    "routines" -> com.lsing.timego.ui.routines.RoutinesScreen()
                }
            }
        }
    }
}

@Composable
private fun TimeGoBottomDock(
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = destinations.indexOfRoute(selectedRoute).coerceAtLeast(0)
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .frosted(shape)
                .padding(6.dp),
        ) {
            val tabWidth = maxWidth / destinations.size
            val indicatorX by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "navIndicatorOffset",
            )

            // The active pill slides between tabs; its offset is read in the layout phase, so the spring
            // never recomposes the dock.
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX.roundToPx(), 0) }
                    .width(tabWidth)
                    .height(56.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(26.dp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val isSelected = selectedRoute == destination.route
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navColor",
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .tactilePress(interactionSource, pressedScale = 0.94f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelectRoute(destination.route) },
                            )
                            .semantics(mergeDescendants = true) {
                                role = Role.Tab
                                selected = isSelected
                                contentDescription = destination.label
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun List<TimeGoDestination>.indexOfRoute(route: String): Int = indexOfFirst { it.route == route }
