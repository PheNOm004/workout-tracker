package com.lsing.timego.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.domain.isCardioOnlySession
import com.lsing.timego.domain.isTrainingSet
import com.lsing.timego.domain.primaryMuscleGroups
import com.lsing.timego.ui.theme.Spacing

/** Title-Case display label for an ExerciseCategory or MuscleGroup enum name, e.g. "FULL_BODY" ->
 *  "Full Body". Shared here since both category and muscle-group headers need it. */
fun formatEnumLabel(rawName: String): String =
    rawName.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** "last 7 days" / "last 30 days" / "last 12 months" -- shared between the Progress screen's own
 *  Muscle Balance card and the Log landing page's, which has its own independent timeframe
 *  selection but needs identical labels. */
fun timeframeLabel(timeframe: ProgressTimeframe): String = when (timeframe) {
    ProgressTimeframe.WEEK -> "last 7 days"
    ProgressTimeframe.MONTH -> "last 30 days"
    ProgressTimeframe.YEAR -> "last 12 months"
}

enum class SessionBodyRegion {
    UPPER_BODY,
    LOWER_BODY,
    CORE,
}

enum class SessionDisplayRegion(
    val label: String,
    val bodyRegion: SessionBodyRegion,
) {
    CHEST("Chest", SessionBodyRegion.UPPER_BODY),
    BACK("Back", SessionBodyRegion.UPPER_BODY),
    SHOULDERS("Shoulders", SessionBodyRegion.UPPER_BODY),
    ARMS("Arms", SessionBodyRegion.UPPER_BODY),
    LEGS("Legs", SessionBodyRegion.LOWER_BODY),
    CORE("Core", SessionBodyRegion.CORE),
}

fun sessionDisplayRegion(group: String, hasBackMuscles: Boolean = false): SessionDisplayRegion? = when (group) {
    MuscleGroup.CHEST.name -> SessionDisplayRegion.CHEST
    MuscleGroup.LATS.name,
    MuscleGroup.UPPER_BACK.name,
    MuscleGroup.LOWER_BACK.name,
    MuscleGroup.TRAPS.name -> SessionDisplayRegion.BACK
    MuscleGroup.REAR_DELTS.name -> if (hasBackMuscles) SessionDisplayRegion.BACK else SessionDisplayRegion.SHOULDERS
    MuscleGroup.FRONT_DELTS.name,
    MuscleGroup.SIDE_DELTS.name -> SessionDisplayRegion.SHOULDERS
    MuscleGroup.BICEPS.name,
    MuscleGroup.TRICEPS.name,
    MuscleGroup.FOREARMS.name -> SessionDisplayRegion.ARMS
    MuscleGroup.QUADS.name,
    MuscleGroup.HAMSTRINGS.name,
    MuscleGroup.GLUTES.name,
    MuscleGroup.ADDUCTORS.name,
    MuscleGroup.CALVES.name -> SessionDisplayRegion.LEGS
    MuscleGroup.ABS.name,
    MuscleGroup.OBLIQUES.name -> SessionDisplayRegion.CORE
    else -> null
}

fun exerciseDisplayRegion(exercise: Exercise, hasBackMuscles: Boolean = false): String {
    if (exercise.category == ExerciseCategory.CARDIO.name) return "Cardio"
    val pGroups = primaryMuscleGroups(exercise).ifEmpty { exercise.muscleGroups.toSet() }
    val region = pGroups.mapNotNull { sessionDisplayRegion(it, hasBackMuscles) }.firstOrNull()
    return region?.label ?: exercise.muscleGroups.firstOrNull()?.let(::formatEnumLabel) ?: "Other"
}

private fun joinDisplayLabels(labels: List<String>): String = when (labels.size) {
    0 -> ""
    1 -> labels[0]
    else -> labels.dropLast(1).joinToString(", ") + " & " + labels.last()
}

/** Formats a set of detailed muscle-group names into a compact session label. Detailed anatomy
 *  remains available to the heatmap and analytics; this summary deliberately groups it into
 *  regions so a pull session such as LATS + UPPER_BACK + BICEPS + FOREARMS is shown as
 *  "Back & Arms", not incorrectly as "Full Body". Full Body is reserved for an explicit
 *  FULL_BODY tag or a set spanning upper body, lower body, and core. Empty input returns "" so
 *  callers can supply their own fallback copy (e.g. "Cardio" or "Light Session"). */
fun formatMuscleGroupList(groups: Collection<String>): String {
    val distinctGroups = groups.distinct()
    if (distinctGroups.isEmpty()) return ""
    if (MuscleGroup.FULL_BODY.name in distinctGroups) return "Full Body"

    val hasBackMuscles = distinctGroups.any {
        it in setOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.LOWER_BACK.name, MuscleGroup.TRAPS.name)
    }
    val regions = distinctGroups
        .mapNotNull { sessionDisplayRegion(it, hasBackMuscles) }
        .distinct()
        .sortedBy { it.ordinal }
    val bodyRegions = regions.map { it.bodyRegion }.toSet()
    if (SessionBodyRegion.UPPER_BODY in bodyRegions &&
        SessionBodyRegion.LOWER_BODY in bodyRegions &&
        SessionBodyRegion.CORE in bodyRegions
    ) {
        return "Full Body"
    }

    // Preserve a readable fallback if a future/custom muscle-group value is not in the compact map.
    if (regions.isEmpty()) return joinDisplayLabels(distinctGroups.map(::formatEnumLabel).sorted())
    return joinDisplayLabels(regions.map { it.label })
}

/** Human-readable "what kind of day was this" label for a session, e.g. "Chest & Triceps". Falls
 *  back to "Cardio" when [isCardioOnly], or "Light Session" when there's simply nothing above the
 *  primary-mover threshold (e.g. only synergist/stabilizer work was logged). Never invents a
 *  muscle group that wasn't actually present in the supplied affected-group set -- see
 *  [formatMuscleGroupList]. */
fun sessionDayLabel(muscleGroups: Set<String>, isCardioOnly: Boolean): String {
    if (isCardioOnly) return "Cardio"
    val listLabel = formatMuscleGroupList(muscleGroups)
    if (listLabel.isNotEmpty()) return listLabel
    return "Light Session"
}

/**
 * Volume-weighted, evidence-based session day label derived from logged sets.
 * Prioritizes dominant trained muscle regions (by set volume), filters out trace finishers/accessories
 * (e.g. 2 sets of hanging at the end of a leg workout), and identifies standard split archetypes
 * (Push, Pull, Legs, Upper Body, Full Body, Shoulders & Arms, etc.).
 */
fun sessionDayLabel(
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    isCardioOnly: Boolean = isCardioOnlySession(sets, exercisesById),
): String {
    if (isCardioOnly) return "Cardio"

    val trainingSets = sets.filter { log ->
        val ex = exercisesById[log.exerciseId] ?: return@filter false
        isTrainingSet(log, ex)
    }
    if (trainingSets.isEmpty()) return "Light Session"

    // Check if session contains any explicit back muscles
    val hasBackMuscles = trainingSets.any { log ->
        val ex = exercisesById[log.exerciseId] ?: return@any false
        val pGroups = primaryMuscleGroups(ex).ifEmpty { ex.muscleGroups.toSet() }
        pGroups.any { it in setOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.LOWER_BACK.name, MuscleGroup.TRAPS.name) }
    }

    val regionCounts = mutableMapOf<SessionDisplayRegion, Int>()
    var chestSets = 0
    var backSets = 0
    var shoulderSets = 0
    var armSets = 0
    var legSets = 0
    var coreSets = 0
    var tricepSets = 0
    var bicepSets = 0

    for (log in trainingSets) {
        val ex = exercisesById[log.exerciseId] ?: continue
        val pGroups = primaryMuscleGroups(ex).ifEmpty { ex.muscleGroups.toSet() }
        val regions = pGroups.mapNotNull { sessionDisplayRegion(it, hasBackMuscles) }.distinct()
        for (region in regions) {
            regionCounts[region] = (regionCounts[region] ?: 0) + 1
            when (region) {
                SessionDisplayRegion.CHEST -> chestSets++
                SessionDisplayRegion.BACK -> backSets++
                SessionDisplayRegion.SHOULDERS -> shoulderSets++
                SessionDisplayRegion.ARMS -> {
                    armSets++
                    if (MuscleGroup.TRICEPS.name in pGroups) tricepSets++
                    if (MuscleGroup.BICEPS.name in pGroups) bicepSets++
                }
                SessionDisplayRegion.LEGS -> legSets++
                SessionDisplayRegion.CORE -> coreSets++
            }
        }
    }

    val totalSetVolume = regionCounts.values.sum()
    if (totalSetVolume == 0) return "Light Session"

    // Filter out minor trace accessories if workout has substantial volume
    // E.g., 2 sets of hanging/curls at the end of a 16-set leg workout shouldn't turn the day into "Legs & Arms"
    val significantRegions = if (totalSetVolume >= 8) {
        regionCounts.filter { (_, count) ->
            count >= 3 || (count.toDouble() / totalSetVolume) >= 0.18
        }.ifEmpty { regionCounts }
    } else {
        regionCounts
    }

    val upperSets = chestSets + backSets + shoulderSets + armSets
    val lowerSets = legSets

    // 1. Full Body: Significant upper and lower body work
    if (legSets >= 4 && (chestSets + backSets) >= 4 && (lowerSets.toDouble() / totalSetVolume) >= 0.28 && (upperSets.toDouble() / totalSetVolume) >= 0.28) {
        return "Full Body"
    }

    // 2. Pure Push: Chest, Front/Side Shoulders, Triceps with no Back and no Legs
    val isPushDominant = (chestSets + shoulderSets + tricepSets) >= (totalSetVolume * 0.8) && backSets == 0 && legSets == 0
    if (isPushDominant) {
        if (chestSets >= 3 && shoulderSets >= 3) return "Push"
        if (chestSets >= 3 && tricepSets >= 3) return "Chest & Arms"
        if (shoulderSets >= 3 && tricepSets >= 3) return "Shoulders & Arms"
        if (chestSets >= 3) return "Chest"
        if (shoulderSets >= 3) return "Shoulders"
    }

    // 3. Pure Pull: Back and Biceps with no Chest and no Legs
    val isPullDominant = (backSets + bicepSets) >= (totalSetVolume * 0.8) && chestSets == 0 && legSets == 0
    if (isPullDominant && backSets >= 3) {
        return if (bicepSets >= 3) "Back & Arms" else "Back"
    }

    // 4. Pure Legs:
    if (legSets >= (totalSetVolume * 0.75)) {
        return if (coreSets >= 3) "Legs & Core" else "Legs"
    }

    // 5. Upper Body: Balanced Chest + Back (+ Arms/Shoulders), no Legs
    if (legSets == 0 && chestSets >= 3 && backSets >= 3) {
        return if (shoulderSets == 0 && armSets == 0) "Chest & Back" else "Upper Body"
    }

    // Sort active regions by set volume descending
    val sortedActiveRegions = significantRegions.entries
        .sortedByDescending { it.value }
        .map { it.key }

    return joinDisplayLabels(sortedActiveRegions.map { it.label })
}

/** Strips hyphens/spaces and lowercases so a search for "pull up" or "pullup" matches an exercise
 *  named "Pull-Up" -- exercise names keep their real punctuation (no renaming), only the search
 *  comparison ignores it. Applied to both the query and the candidate name. */
private fun normalizeForSearch(text: String): String =
    text.lowercase().filterNot { it == '-' || it == ' ' }

internal const val EXERCISE_SEARCH_RESULT_LIMIT = 40

/** Keeps the caller's exercise order (frequency order on the Log screen) while bounding a broad
 * query so the exercise browser never eagerly composes the whole catalog at once. */
internal fun boundedExerciseSearch(exercises: List<Exercise>, query: String): List<Exercise> {
    val normalizedQuery = normalizeForSearch(query)
    if (normalizedQuery.isBlank()) return emptyList()
    return exercises
        .asSequence()
        .filter { normalizeForSearch(it.name).contains(normalizedQuery) }
        .take(EXERCISE_SEARCH_RESULT_LIMIT)
        .toList()
}

enum class MuscleFilterOption(val label: String) {
    ALL("All"),
    FAVORITES("★ Favorites"),
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    LEGS("Legs"),
    CORE("Core"),
    CARDIO("Cardio"),
}

/** Flat A-Z exercise browser with a bounded viewport and visible right-hand scroll thumb. */
@Composable
private fun AlphabeticalExerciseList(
    exercises: List<Exercise>,
    itemContent: @Composable (Exercise) -> Unit,
) {
    val listState = rememberLazyListState()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp, max = 520.dp),
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val layoutInfo = listState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        val averageItemHeightPx = layoutInfo.visibleItemsInfo
            .map { it.size.toFloat() }
            .average()
            .toFloat()
            .takeIf { it.isFinite() && it > 0f }
            ?: with(density) { 48.dp.toPx() }
        val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
            .toFloat()
            .coerceAtLeast(1f)
        val contentHeightPx = (averageItemHeightPx * totalItems).coerceAtLeast(viewportHeightPx)
        val contentScrollRangePx = (contentHeightPx - viewportHeightPx).coerceAtLeast(1f)
        val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
        val currentScrollPx = if (firstVisible == null) 0f else {
            firstVisible.index * averageItemHeightPx - firstVisible.offset
        }
        val scrollFraction = if (contentScrollRangePx <= 1f) 0f else {
            currentScrollPx / contentScrollRangePx
        }
        val thumbHeightPx = (trackHeightPx * (viewportHeightPx / contentHeightPx)).coerceIn(
            with(density) { 40.dp.toPx() },
            with(density) { 96.dp.toPx() },
        )
        val thumbOffsetPx = if (totalItems <= visibleItems) 0f else {
            (trackHeightPx - thumbHeightPx) * scrollFraction.coerceIn(0f, 1f)
        }

        /*
         * The picker uses a bounded LazyColumn so switching order only composes the visible rows.
         * A plain Column here made the entire catalogue compose eagerly and caused multi-second
         * A-Z transitions on the phone.
         */
        val sections = remember(exercises) {
            exercises
                .groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
                .toSortedMap()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
        ) {
            sections.forEach { (letter, sectionExercises) ->
                item(key = "section-$letter") {
                    Text(
                        letter,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.Small, bottom = Spacing.ExtraSmall)
                            .semantics { contentDescription = "Alphabetical section $letter" },
                    )
                }
                items(sectionExercises, key = { it.id }) { exercise ->
                    itemContent(exercise)
                }
            }
        }

        if (totalItems > visibleItems) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(48.dp)
                    .zIndex(2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                    .pointerInput(totalItems, contentScrollRangePx, trackHeightPx, thumbHeightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val thumbTravelRange = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
                            val contentDelta = dragAmount.y / thumbTravelRange * contentScrollRangePx
                            listState.dispatchRawDelta(contentDelta)
                        }
                    }
                    .semantics { contentDescription = "Alphabetical exercise list scrollbar" },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = with(density) { thumbOffsetPx.toDp() })
                    .width(8.dp)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
            )
        }
    }
}

enum class ExerciseListOrder(val label: String) {
    CURRENT("Current"),
    ALPHABETICAL("A–Z"),
}

internal fun orderExercises(exercises: List<Exercise>, order: ExerciseListOrder): List<Exercise> =
    if (order == ExerciseListOrder.ALPHABETICAL) {
        exercises.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    } else {
        exercises
    }

fun exerciseMatchesFilter(exercise: Exercise, filter: MuscleFilterOption, favoriteIds: Set<Long> = emptySet()): Boolean {
    return when (filter) {
        MuscleFilterOption.ALL -> true
        MuscleFilterOption.FAVORITES -> exercise.id in favoriteIds
        MuscleFilterOption.CARDIO -> exercise.category == ExerciseCategory.CARDIO.name
        MuscleFilterOption.CHEST -> exercise.muscleGroups.any { sessionDisplayRegion(it) == SessionDisplayRegion.CHEST }
        MuscleFilterOption.BACK -> exercise.muscleGroups.any { sessionDisplayRegion(it) == SessionDisplayRegion.BACK || it == MuscleGroup.REAR_DELTS.name }
        MuscleFilterOption.SHOULDERS -> exercise.muscleGroups.any { sessionDisplayRegion(it) == SessionDisplayRegion.SHOULDERS }
        MuscleFilterOption.ARMS -> exercise.muscleGroups.any { sessionDisplayRegion(it) == SessionDisplayRegion.ARMS }
        MuscleFilterOption.LEGS -> exercise.muscleGroups.any { sessionDisplayRegion(it) == SessionDisplayRegion.LEGS }
        MuscleFilterOption.CORE -> exercise.muscleGroups.any { sessionDisplayRegion(it) == SessionDisplayRegion.CORE }
    }
}

/** Renders [exercises] with a search box and sticky muscle filter chips. */
@Composable
fun ExerciseSections(
    exercises: List<Exercise>,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    showSearchField: Boolean = true,
    listOrder: ExerciseListOrder = ExerciseListOrder.CURRENT,
    onListOrderChange: ((ExerciseListOrder) -> Unit)? = null,
    selectedFilter: MuscleFilterOption = MuscleFilterOption.ALL,
    onSelectFilter: ((MuscleFilterOption) -> Unit)? = null,
    favoriteExerciseIds: Set<Long> = emptySet(),
    itemContent: @Composable (Exercise) -> Unit,
) {
    var localQuery by remember { mutableStateOf("") }
    val query = searchQuery ?: localQuery
    var localFilter by remember { mutableStateOf(MuscleFilterOption.ALL) }
    val currentFilter = if (onSelectFilter != null) selectedFilter else localFilter
    val orderedExercises = remember(exercises, listOrder) { orderExercises(exercises, listOrder) }
    val setFilter: (MuscleFilterOption) -> Unit = { filter ->
        if (onSelectFilter != null) onSelectFilter(filter) else localFilter = filter
    }
    var expandedGroupKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    val setQuery: (String) -> Unit = { value -> onSearchQueryChange?.invoke(value) ?: run { localQuery = value } }

    if (showSearchField) {
        OutlinedTextField(
            value = query,
            onValueChange = setQuery,
            label = { Text("Search exercises") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { setQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
        )
    }

    if (onListOrderChange != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text("Order", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExerciseListOrder.entries.forEach { option ->
                FilterChip(
                    selected = listOrder == option,
                    onClick = { onListOrderChange.invoke(option) },
                    label = { Text(option.label) },
                )
            }
        }
    }

    // Muscle Filter Pills Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Small)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        MuscleFilterOption.entries.forEach { option ->
            if (option == MuscleFilterOption.FAVORITES && favoriteExerciseIds.isEmpty()) return@forEach
            FilterChip(
                selected = currentFilter == option,
                onClick = { setFilter(option) },
                label = { Text(option.label) },
            )
        }
    }

    if (query.isNotBlank()) {
        val totalMatches = remember(orderedExercises, query) {
            val normalizedQuery = normalizeForSearch(query)
            orderedExercises.count { normalizeForSearch(it.name).contains(normalizedQuery) }
        }
        val matches = remember(orderedExercises, query) { boundedExerciseSearch(orderedExercises, query) }
        if (listOrder == ExerciseListOrder.ALPHABETICAL) {
            AlphabeticalExerciseList(matches, itemContent)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 520.dp),
            ) {
                items(matches, key = { it.id }) { exercise -> itemContent(exercise) }
            }
        }
        if (totalMatches > matches.size) {
            Text(
                "Showing the first ${matches.size} matches. Refine your search to narrow the list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }
        return
    }

    if (currentFilter != MuscleFilterOption.ALL) {
        val filtered = remember(orderedExercises, currentFilter, favoriteExerciseIds) {
            orderedExercises.filter { exerciseMatchesFilter(it, currentFilter, favoriteExerciseIds) }
        }
        if (filtered.isEmpty()) {
            Text(
                "No exercises found for ${currentFilter.label}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.Medium),
            )
        } else {
            if (listOrder == ExerciseListOrder.ALPHABETICAL) {
                AlphabeticalExerciseList(filtered, itemContent)
            } else {
                val bySubGroup = remember(filtered) {
                    filtered.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 520.dp),
                ) {
                    bySubGroup.forEach { (subGroup, groupExercises) ->
                        item(key = "filter-$subGroup") {
                            Text(
                                formatEnumLabel(subGroup),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = Spacing.Medium, bottom = Spacing.ExtraSmall),
                            )
                        }
                        items(groupExercises, key = { it.id }) { exercise -> itemContent(exercise) }
                    }
                }
            }
        }
        return
    }

    if (listOrder == ExerciseListOrder.ALPHABETICAL) {
        AlphabeticalExerciseList(orderedExercises, itemContent)
        return
    }

    val byCategory = remember(orderedExercises) { orderedExercises.groupBy { it.category } }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 520.dp),
    ) {
        ExerciseCategory.entries.forEach { category ->
            val inCategory = byCategory[category.name].orEmpty()
            if (inCategory.isEmpty()) return@forEach
            item(key = "category-${category.name}") {
                var expanded by remember(category) { mutableStateOf(false) }
                val catBringIntoView = remember { BringIntoViewRequester() }
                LaunchedEffect(expanded) {
                    if (expanded) catBringIntoView.bringIntoView()
                }
                val catChevronRotation by animateFloatAsState(
                    targetValue = if (expanded) 90f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.EaseInOut),
                    label = "catChevronRotation",
                )
                Column(
                    modifier = Modifier.fillMaxWidth().bringIntoViewRequester(catBringIntoView),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.Large, bottom = Spacing.ExtraSmall)
                            .clickable { expanded = !expanded },
                    ) {
                        val visual = categoryVisual(category)
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = visual.accent,
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp).graphicsLayer { rotationZ = catChevronRotation },
                        )
                        Text(formatEnumLabel(category.name), style = MaterialTheme.typography.titleMedium)
                    }
                    AnimatedExpand(expanded) {
                        val byMuscleGroup = remember(inCategory) {
                            inCategory.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
                        }
                        byMuscleGroup.forEach { (group, groupExercises) ->
                            val groupKey = "${category.name}:$group"
                            val groupExpanded = groupKey in expandedGroupKeys
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.Small)
                                    .clickable { expandedGroupKeys = toggleExpandedExerciseGroupKeys(expandedGroupKeys, groupKey) },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = if (groupExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.padding(start = 32.dp, end = 4.dp),
                                )
                                Text(formatEnumLabel(group), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AnimatedExpand(groupExpanded) {
                                groupExercises.forEach { exercise -> itemContent(exercise) }
                            }
                        }
                    }
                }
            }
        }
    }
}
