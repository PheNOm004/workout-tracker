package com.lsing.timego.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
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
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        entries.forEach { entry -> MuscleBalanceBarColumn(entry) }
    }
}

@Composable
private fun MuscleBalanceBarColumn(entry: MuscleBalanceBarEntry) {
    val current = entry.current
    val delta = current?.let { value -> entry.previous?.let { value - it } }
    val deltaValue = delta ?: 0f
    val deltaColor = when {
        current == null || abs(deltaValue) < 0.005f -> MaterialTheme.colorScheme.onSurfaceVariant
        entry.previous == null -> MaterialTheme.colorScheme.primary
        deltaValue > 0f -> NightMint
        else -> MaterialTheme.colorScheme.error
    }
    val currentLabel = current?.let { "${(it * 100).roundToInt()}%" } ?: "—"
    val deltaLabel = when {
        current == null -> "No data"
        entry.previous == null -> "New"
        abs(deltaValue) < 0.005f -> "—"
        deltaValue > 0f -> "▲ ${(deltaValue * 100).roundToInt()} pp"
        else -> "▼ ${(-deltaValue * 100).roundToInt()} pp"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        // 48dp was too narrow for the longest single-word labels ("Hamstrings", "Adductors") --
        // with no space to wrap at, Compose broke them mid-word instead. 56dp fits those on one
        // line while staying well under the original 64dp.
        modifier = Modifier.width(56.dp),
    ) {
        Text(
            text = currentLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = LedgerMonoFamily),
            color = if (current == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            deltaLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = LedgerMonoFamily),
            color = deltaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (current != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(current.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Text(
            entry.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(30.dp),
        )
    }
}
