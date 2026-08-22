package com.lsing.timego.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.theme.LedgerMonoFamily
import com.lsing.timego.ui.theme.NightMint
import com.lsing.timego.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

data class MuscleBalanceBarEntry(
    val label: String,
    val current: Float?,
    val previous: Float?,
)

/** Ranks active muscle groups first by their target attainment. Absent groups deliberately stay
 * null instead of being represented as 0%, because no training data is different from a measured
 * zero. */
fun rankedMuscleBalanceBars(
    current: Map<String, Float>,
    previous: Map<String, Float>,
): List<MuscleBalanceBarEntry> =
    MuscleGroup.entries
        .filterNot { it == MuscleGroup.FULL_BODY }
        .map { group ->
            MuscleBalanceBarEntry(
                label = formatEnumLabel(group.name),
                current = current[group.name],
                previous = previous[group.name],
            )
        }
        .sortedWith(compareByDescending<MuscleBalanceBarEntry> { it.current ?: -1f }.thenBy { it.label })

@Composable
fun MuscleBalanceBars(entries: List<MuscleBalanceBarEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        entries.forEach { entry -> MuscleBalanceBarRow(entry) }
    }
}

@Composable
private fun MuscleBalanceBarRow(entry: MuscleBalanceBarEntry) {
    val current = entry.current
    val delta = current?.let { value -> entry.previous?.let { value - it } }
    val deltaColor = when {
        delta == null || abs(delta) < 0.005f -> MaterialTheme.colorScheme.onSurfaceVariant
        delta > 0f -> NightMint
        else -> MaterialTheme.colorScheme.error
    }
    val deltaLabel = when {
        delta == null || abs(delta) < 0.005f -> "—"
        delta > 0f -> "▲ ${(delta * 100).roundToInt()} pp"
        else -> "▼ ${(-delta * 100).roundToInt()} pp"
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(
                text = current?.let { "${(it * 100).roundToInt()}%" } ?: "NO DATA",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = LedgerMonoFamily),
                color = if (current == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                deltaLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = LedgerMonoFamily),
                color = deltaColor,
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (current != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(current.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
