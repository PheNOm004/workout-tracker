package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.domain.DayTrainingStats
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.NightDeckHigh
import com.lsing.timego.ui.theme.NightEyebrow
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val BREAKDOWN_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

/** Per-day breakdown behind tapping any of the Consistency stat tiles (Workouts/Duration/Volume/
 *  Sets) -- the same underlying [DayTrainingStats] list regardless of which tile was tapped, since
 *  seeing all four together is strictly more useful than a single-metric view, and it avoids four
 *  near-identical dialogs. Days with zero training don't appear -- [days] only ever contains days
 *  that had a session, same absence-not-zero convention the heatmap and [WorkoutHistoryDialog] use. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodBreakdownDialog(periodLabel: String, days: List<DayTrainingStats>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightDeckHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "BREAKDOWN",
                style = NightEyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (days.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                    ) {
                        Text(
                            text = "No sessions logged in this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        days.forEachIndexed { index, day ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = day.date.format(BREAKDOWN_DATE_FORMATTER),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                                    Text(
                                        text = "${day.workouts} workout${if (day.workouts == 1) "" else "s"} · " +
                                            "${formatHistoryDuration(day.durationMinutes)} · " +
                                            "${day.volumeKg.toInt()} kg · " +
                                            "${day.sets} set${if (day.sets == 1) "" else "s"}",
                                        style = LedgerFigureValue.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                Text("Close")
            }
        }
    }
}
