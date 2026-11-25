package com.lsing.timego.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.data.Exercise
import com.lsing.timego.domain.PrType
import com.lsing.timego.domain.RecordEntry
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.PlateYellow
import com.lsing.timego.ui.theme.Spacing

private const val COLLAPSED_RECORDS = 6

/** All records at a glance, two to a row; ones set inside the selected period are outlined and counted. */
@Composable
fun RecordsCard(entries: List<RecordEntry>, exercisesById: Map<Long, Exercise>) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val newCount = entries.count { it.isNew }
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)) {
        Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (newCount > 0) {
                Text("$newCount new", style = MaterialTheme.typography.labelMedium, color = PlateYellow)
            }
            val shown = if (showAll) entries else entries.take(COLLAPSED_RECORDS)
            // An outline on every tile marks nothing out, so it only appears beside older records.
            val outlineNew = shown.any { !it.isNew }
            shown.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { entry -> RecordTile(entry, exercisesById[entry.record.exerciseId]?.name.orEmpty(), outlineNew, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (entries.size > COLLAPSED_RECORDS) {
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Show fewer" else "Show all ${entries.size}") }
            }
        }
    }
}

@Composable
private fun RecordTile(entry: RecordEntry, name: String, outlineNew: Boolean, modifier: Modifier) {
    val record = entry.record
    val value = if (record.type == PrType.LONGEST_HOLD) {
        "${record.value.toInt()}s hold"
    } else {
        val weight = record.addedWeightKg?.let(::formatCalisthenicsWeight) ?: "%.1fkg".format(record.value)
        "$weight \u00d7 ${record.secondaryValue?.toInt() ?: 0}"
    }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (entry.isNew && outlineNew) Modifier.border(1.5.dp, PlateYellow, shape) else Modifier)
            .semantics(mergeDescendants = true) { if (entry.isNew) contentDescription = "New record, $name, $value" }
            .padding(12.dp),
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = LedgerFigureValue.copy(fontSize = 16.sp), color = if (entry.isNew) PlateYellow else MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}
