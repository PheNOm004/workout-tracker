package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lsing.timego.ui.theme.NightEyebrow
import com.lsing.timego.ui.theme.Spacing

/**
 * The Gauge Panel's signature hardware dialog plate.
 *
 * Replaces raw Material3 [AlertDialog] so all popups app-wide inherit the Gauge Panel
 * design language:
 * - [SurfaceCard] plate with [NightDeckHigh] steel background
 * - Real cast shadow elevation
 * - Brass-lit top bezel reflection ([NightSheenTop])
 * - Precision 1.dp hairline border ([NightEdgeHairline])
 * - Signature [NightEyebrow] uppercase accent header
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
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        SurfaceCard(
            hero = true,
            cornerRadius = cornerRadius,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp),
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

                content()

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
