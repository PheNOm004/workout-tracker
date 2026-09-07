package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lsing.timego.ui.theme.NightEdgeHairline
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Lifted and adapted from HeatP's SummaryScreen.kt -- self-contained, no ViewModel
 *  coupling, takes only a plain [ratios] map, so it serves TimeGo's consistency heatmap unchanged.
 *  Renders either "Last 18 weeks" (fills screen width, no scroll) or the full current calendar
 *  year (horizontally scrollable, auto-scrolled to today). Week columns are Monday-start.
 *  Transition glides smoothly both ways using EaseInOut curves matching the app's physical design language.
 *  [lightColor]/[darkColor] color the completion lerp. A date absent from [ratios] (no session
 *  logged / before the app existed) renders the same neutral gray as a real 0% ratio.
 *  [onDateClick], when non-null, makes every past/today dot tappable (future dates never are,
 *  there's nothing to show) -- lets a caller show that day's detailed workout history. */
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.sp

@Composable
fun HeatmapGrid(ratios: Map<LocalDate, Float>, lightColor: Color, darkColor: Color, onDateClick: ((LocalDate) -> Unit)? = null) {
    var showFullYear by remember { mutableStateOf(false) }
    // The year grid stays composed until its scroll-back finishes, so leaving it also glides.
    var yearMounted by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val displayLocale = Locale.forLanguageTag(
        LocalConfiguration.current.locales.toLanguageTags().substringBefore(','),
    )
    val currentWeekMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val yearStartMonday = LocalDate.of(today.year, 1, 1).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    val weeksInYear = ((LocalDate.of(today.year, 12, 31).toEpochDay() - yearStartMonday.toEpochDay()) / 7 + 1).toInt()
    val todayWeekIndex = ((currentWeekMonday.toEpochDay() - yearStartMonday.toEpochDay()) / 7).toInt()
    val scrollState = rememberScrollState()
    val glide = tween<Float>(durationMillis = 480, easing = EaseInOut)
    val sizeGlide = tween<IntSize>(durationMillis = 480, easing = EaseInOut)
    val spacing = 3.dp

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
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
            val availableWidthPx = constraints.maxWidth
            val density = LocalDensity.current
            val spacingPx = with(density) { spacing.roundToPx() }
            val dotPx = (availableWidthPx - spacingPx * 17) / 18
            val dotSize = with(density) { dotPx.toDp() }
            val stepPx = dotPx + spacingPx

            val todayLeftPx = todayWeekIndex * stepPx
            // Entry/exit: today's column flush to the right edge — exactly where it sits in the 18-week view,
            // so the layout swap is seamless. Rest: today's column centred.
            val edgeOffset = (todayLeftPx - (availableWidthPx - stepPx)).coerceAtLeast(0)
            val centeredOffset = (todayLeftPx - availableWidthPx / 2 + stepPx / 2).coerceAtLeast(0)

            LaunchedEffect(showFullYear) {
                if (showFullYear) {
                    scrollState.scrollTo(edgeOffset)
                    yearMounted = true
                    snapshotFlow { scrollState.maxValue }.first { it < Int.MAX_VALUE }
                    scrollState.scrollTo(edgeOffset)
                    scrollState.animateScrollTo(centeredOffset, animationSpec = glide)
                } else if (yearMounted) {
                    scrollState.animateScrollTo(edgeOffset, animationSpec = glide)
                    yearMounted = false
                }
            }

            if (yearMounted) {
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        for (week in 0 until weeksInYear) {
                            val weekStart = yearStartMonday.plusWeeks(week.toLong())
                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                HeatmapWeekDots(weekStart, today, ratios, lightColor, darkColor, Modifier.size(dotSize), onDateClick)
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showFullYear,
                        enter = fadeIn(glide) + expandVertically(sizeGlide),
                        exit = fadeOut(glide) + shrinkVertically(sizeGlide),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                for (week in 0 until weeksInYear) {
                                    val weekStart = yearStartMonday.plusWeeks(week.toLong())
                                    val isMonthStart = week == 0 || weekStart.month != yearStartMonday.plusWeeks((week - 1).toLong()).month
                                    Box(modifier = Modifier.width(dotSize)) {
                                        if (isMonthStart) {
                                            Text(
                                                weekStart.month.getDisplayName(TextStyle.SHORT, displayLocale),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                ) {
                    for (week in 0 until 18) {
                        val weekStart = currentWeekMonday.minusWeeks(17).plusWeeks(week.toLong())
                        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                            HeatmapWeekDots(weekStart, today, ratios, lightColor, darkColor, Modifier.size(dotSize), onDateClick)
                        }
                    }
                }
            }
        }
    }
}

/** One dot per day of [weekStart]'s week -- color is a [darkColor]-to-[lightColor] lerp by that
 *  day's ratio (dark background: low value blends in, high value pops). A future date, a date
 *  absent from [ratios] (no session logged), or a real 0% ratio all render the same neutral gray.
 *
 *  Every dot shares the same geometry (all callers pass the same [modifier]), but a bright
 *  fill on a dark ground reads as visibly larger than a dark fill of identical size -- the eye's
 *  own irradiation illusion, not a layout bug -- which is what made high-ratio dots look bigger
 *  than neutral ones. A shared hairline stroke anchors the true edge for every dot regardless of
 *  fill brightness, so the grid reads as uniform circles rather than illusion-skewed blobs. */
@Composable
private fun HeatmapWeekDots(
    weekStart: LocalDate,
    today: LocalDate,
    ratios: Map<LocalDate, Float>,
    lightColor: Color,
    darkColor: Color,
    modifier: Modifier = Modifier,
    onDateClick: ((LocalDate) -> Unit)? = null,
) {
    for (dayOffset in 0 until 7) {
        val date = weekStart.plusDays(dayOffset.toLong())
        val ratio = ratios[date]
        val clickModifier = if (onDateClick != null && !date.isAfter(today)) {
            Modifier.clickable { onDateClick(date) }
        } else {
            Modifier
        }
        val edge = Modifier.border(0.75.dp, NightEdgeHairline, CircleShape)
        if (date.isAfter(today) || ratio == null || ratio <= 0f) {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    // Neutral still means no recorded training, never a failure; it is simply
                    // lifted above the Backlit ground enough to make the calendar structure legible.
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f))
                    .then(edge)
                    .then(clickModifier),
            )
        } else {
            val cellColor = lerp(darkColor, lightColor, ratio.coerceIn(0f, 1f))
            Box(modifier = modifier.clip(CircleShape).background(cellColor).then(edge).then(clickModifier))
        }
    }
}
