package com.lsing.timego.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Lifted verbatim from HeatP's SummaryScreen.kt (2026-08-09) -- self-contained, no ViewModel
 *  coupling, takes only a plain [ratios] map, so it serves TimeGo's consistency heatmap unchanged.
 *  Renders either "Last 18 weeks" (fills screen width, no scroll) or the full current calendar
 *  year (horizontally scrollable, auto-scrolled to today). Week columns are Monday-start.
 *  [lightColor]/[darkColor] color the completion lerp. A date absent from [ratios] (no session
 *  logged / before the app existed) renders the same neutral gray as a real 0% ratio. */
@Composable
fun HeatmapGrid(ratios: Map<LocalDate, Float>, lightColor: Color, darkColor: Color) {
    var showFullYear by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val currentWeekMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val yearStartMonday = LocalDate.of(today.year, 1, 1).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    val weeksInYear = ((LocalDate.of(today.year, 12, 31).toEpochDay() - yearStartMonday.toEpochDay()) / 7 + 1).toInt()
    val todayWeekIndex = ((currentWeekMonday.toEpochDay() - yearStartMonday.toEpochDay()) / 7).toInt()
    val scrollState = rememberScrollState()
    LaunchedEffect(showFullYear, scrollState.maxValue) {
        if (showFullYear && scrollState.maxValue > 0) {
            val fraction = todayWeekIndex.toFloat() / (weeksInYear - 1).coerceAtLeast(1)
            scrollState.scrollTo((scrollState.maxValue * fraction).toInt())
        }
    }
    val spacing = 3.dp

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (showFullYear) "${today.year} (scrollable)" else "Last 18 weeks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showFullYear = !showFullYear }) {
                Text(if (showFullYear) "Last 18 weeks" else "${today.year} (scrollable)", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val dotSize = (maxWidth - spacing * 17) / 18
            if (showFullYear) {
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        for (week in 0 until weeksInYear) {
                            val weekStart = yearStartMonday.plusWeeks(week.toLong())
                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                HeatmapWeekDots(weekStart, today, ratios, lightColor, darkColor, Modifier.size(dotSize))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        for (week in 0 until weeksInYear) {
                            val weekStart = yearStartMonday.plusWeeks(week.toLong())
                            val isMonthStart = week == 0 || weekStart.month != yearStartMonday.plusWeeks((week - 1).toLong()).month
                            Box(modifier = Modifier.width(dotSize)) {
                                if (isMonthStart) {
                                    Text(
                                        weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (week in 0 until 18) {
                        val weekStart = currentWeekMonday.minusWeeks(17).plusWeeks(week.toLong())
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            HeatmapWeekDots(weekStart, today, ratios, lightColor, darkColor, Modifier.fillMaxWidth().aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

/** One dot per day of [weekStart]'s week -- color is a [darkColor]-to-[lightColor] lerp by that
 *  day's ratio (dark background: low value blends in, high value pops). A future date, a date
 *  absent from [ratios] (no session logged), or a real 0% ratio all render the same neutral gray. */
@Composable
private fun HeatmapWeekDots(
    weekStart: LocalDate,
    today: LocalDate,
    ratios: Map<LocalDate, Float>,
    lightColor: Color,
    darkColor: Color,
    dotModifier: Modifier,
) {
    for (dayOffset in 0 until 7) {
        val date = weekStart.plusDays(dayOffset.toLong())
        val ratio = ratios[date]
        if (date.isAfter(today) || ratio == null || ratio <= 0f) {
            Box(
                modifier = dotModifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            )
        } else {
            val cellColor = lerp(darkColor, lightColor, ratio.coerceIn(0f, 1f))
            Box(modifier = dotModifier.clip(CircleShape).background(cellColor))
        }
    }
}
