package com.lsing.timego.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.PlateArms
import com.lsing.timego.ui.theme.PlateBlue
import com.lsing.timego.ui.theme.PlateGreen
import com.lsing.timego.ui.theme.PlateRed
import com.lsing.timego.ui.theme.PlateSteel
import com.lsing.timego.ui.theme.PlateYellow

/** The plate colour that identifies a body region everywhere it appears: chest red, back blue, shoulders
 *  yellow, legs green, arms white, core and anything else steel. */
@Composable
fun regionColor(label: String): Color = when (label) {
    SessionDisplayRegion.CHEST.label -> PlateRed
    SessionDisplayRegion.BACK.label -> PlateBlue
    SessionDisplayRegion.SHOULDERS.label -> PlateYellow
    SessionDisplayRegion.LEGS.label -> PlateGreen
    SessionDisplayRegion.ARMS.label -> PlateArms
    else -> PlateSteel
}

/** A region name with its plate-colour dot. */
@Composable
fun RegionChip(label: String, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(regionColor(label)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegionChipRow(labels: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { RegionChip(it) }
    }
}
