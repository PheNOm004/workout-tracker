package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lsing.timego.ui.theme.NightEyebrow
import com.lsing.timego.ui.theme.Spacing

/** Provides access to the animated dismiss handler for [TimeGoDialog]. */
val LocalTimeGoDialogDismiss = compositionLocalOf<() -> Unit> { {} }

/**
 * The Gauge Panel's signature hardware dialog plate.
 *
 * Features:
 * - [SurfaceCard] plate with [NightDeckHigh] steel background and [riveted] brass fasteners
 * - Real cast shadow elevation
 * - Brass-lit top bezel reflection ([NightSheenTop])
 * - Precision 1.dp hairline border ([NightEdgeHairline])
 * - Signature [NightEyebrow] uppercase accent header
 * - Symmetric EaseInOut enter/exit animations (scale + alpha) matching heatmap glide
 * - Animated scrim fade on entrance and dismissal
 * - Built-in scroll-to-fit constraint and IME keyboard avoidance
 */
@Composable
fun TimeGoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    title: String? = null,
    subtitle: String? = null,
    headerContent: (@Composable () -> Unit)? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    ),
    content: @Composable () -> Unit,
) {
    val transitionState = remember { MutableTransitionState(false) }.apply {
        targetState = true
    }
    var isDismissing by remember { mutableStateOf(false) }

    val animateAndDismiss: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            transitionState.targetState = false
        }
    }

    LaunchedEffect(transitionState.isIdle, transitionState.currentState) {
        if (isDismissing && transitionState.isIdle && !transitionState.currentState) {
            onDismissRequest()
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (transitionState.targetState) 0.65f else 0f,
        animationSpec = tween(durationMillis = 260, easing = EaseInOut),
        label = "dialogScrimAlpha",
    )

    Dialog(
        onDismissRequest = { animateAndDismiss() },
        properties = properties,
    ) {
        CompositionLocalProvider(LocalTimeGoDialogDismiss provides animateAndDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { animateAndDismiss() },
                    )
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = scaleIn(
                        animationSpec = tween(durationMillis = 280, easing = EaseInOut),
                        initialScale = 0.90f,
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 240, easing = EaseInOut),
                    ),
                    exit = scaleOut(
                        animationSpec = tween(durationMillis = 240, easing = EaseInOut),
                        targetScale = 0.90f,
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200, easing = EaseInOut),
                    ),
                ) {
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    SurfaceCard(
                        hero = true,
                        riveted = true,
                        cornerRadius = cornerRadius,
                        modifier = modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 480.dp)
                            .heightIn(max = screenHeight * 0.85f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}, // Consume clicks inside the card so they don't dismiss
                            ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.Large),
                        ) {
                            if (headerContent != null) {
                                headerContent()
                                Spacer(modifier = Modifier.height(Spacing.Medium))
                            } else if (title != null || eyebrow != null || subtitle != null) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (eyebrow != null) {
                                        Text(
                                            text = eyebrow.uppercase(),
                                            style = NightEyebrow,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    if (title != null) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = if (eyebrow != null) 2.dp else 0.dp),
                                        )
                                    }
                                    if (subtitle != null) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.Medium))
                            }

                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                content()
                            }

                            if (confirmButton != null || dismissButton != null) {
                                Spacer(modifier = Modifier.height(Spacing.Large))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (dismissButton != null) {
                                        dismissButton()
                                    }
                                    if (confirmButton != null) {
                                        confirmButton()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
