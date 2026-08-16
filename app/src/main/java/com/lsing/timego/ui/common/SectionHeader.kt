package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lsing.timego.ui.theme.Spacing

/** Shared section-title row used across Log/Routines/Progress, with an optional trailing slot
 *  for an inline action (e.g. Routines' "+ New routine" button) -- replaces one-off
 *  Row+Text+Button combos doing the same job on each screen. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = Spacing.Large,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(top = topPadding, bottom = Spacing.Small),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}
