package com.lsing.timego.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    selectedFilter: MuscleFilterOption = MuscleFilterOption.ALL,
    onSelectFilter: ((MuscleFilterOption) -> Unit)? = null,
    favoriteExerciseIds: Set<Long> = emptySet(),
    itemContent: @Composable (Exercise) -> Unit,
) {
    var localQuery by remember { mutableStateOf("") }
    val query = searchQuery ?: localQuery
    var localFilter by remember { mutableStateOf(MuscleFilterOption.ALL) }
    val currentFilter = if (onSelectFilter != null) selectedFilter else localFilter
    val setFilter: (MuscleFilterOption) -> Unit = { filter ->
        if (onSelectFilter != null) onSelectFilter(filter) else localFilter = filter
    }
    var expandedGroupKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    val setQuery: (String) -> Unit = { value -> onSearchQueryChange?.invoke(value) ?: run { localQuery = value } }

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
        val totalMatches = remember(exercises, query) {
            val normalizedQuery = normalizeForSearch(query)
            exercises.count { normalizeForSearch(it.name).contains(normalizedQuery) }
        }
        val matches = remember(exercises, query) { boundedExerciseSearch(exercises, query) }
        matches.forEach { exercise -> itemContent(exercise) }
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
        val filtered = remember(exercises, currentFilter, favoriteExerciseIds) {
            exercises.filter { exerciseMatchesFilter(it, currentFilter, favoriteExerciseIds) }
        }
        if (filtered.isEmpty()) {
            Text(
                "No exercises found for ${currentFilter.label}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.Medium),
            )
        } else {
            // Group by sub-muscle group or render direct list with fast access
            val bySubGroup = remember(filtered) {
                filtered.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
            }
            bySubGroup.forEach { (subGroup, groupExercises) ->
                Text(
                    formatEnumLabel(subGroup),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.Medium, bottom = Spacing.ExtraSmall),
                )
                groupExercises.forEach { exercise -> itemContent(exercise) }
            }
        }
        return
    }

    val byCategory = remember(exercises) { exercises.groupBy { it.category } }
    ExerciseCategory.entries.forEach { category ->
        val inCategory = byCategory[category.name].orEmpty()
        if (inCategory.isEmpty()) return@forEach
        key(category) {
            var expanded by remember(category) { mutableStateOf(false) }
            val catBringIntoView = remember { BringIntoViewRequester() }
            LaunchedEffect(expanded) {
                if (expanded) {
                    kotlinx.coroutines.delay(120)
                    catBringIntoView.bringIntoView()
                    kotlinx.coroutines.delay(220)
                    catBringIntoView.bringIntoView()
                }
            }
            val catChevronRotation by animateFloatAsState(
                targetValue = if (expanded) 90f else 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "catChevronRotation",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(catBringIntoView)
                    .padding(top = Spacing.Large, bottom = Spacing.ExtraSmall)
                    .clickable { expanded = !expanded },
            ) {
                val visual = categoryVisual(category)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = visual.accent,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 4.dp)
                        .graphicsLayer { rotationZ = catChevronRotation },
                )
                Text(
                    formatEnumLabel(category.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            AnimatedExpand(expanded) {
                val byMuscleGroup = remember(inCategory) {
                    inCategory.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
                }
                byMuscleGroup.forEach { (group, groupExercises) ->
                    key(group) {
                        val groupKey = "${category.name}:$group"
                        val groupExpanded = groupKey in expandedGroupKeys
                        val groupBringIntoView = remember { BringIntoViewRequester() }
                        LaunchedEffect(groupExpanded) {
                            if (groupExpanded) {
                                kotlinx.coroutines.delay(120)
                                groupBringIntoView.bringIntoView()
                                kotlinx.coroutines.delay(220)
                                groupBringIntoView.bringIntoView()
                            }
                        }
                        val subChevronRotation by animateFloatAsState(
                            targetValue = if (groupExpanded) 90f else 0f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                            label = "subChevronRotation",
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(groupBringIntoView)
                                .padding(top = Spacing.Small)
                                .clickable {
                                    expandedGroupKeys = toggleExpandedExerciseGroupKeys(expandedGroupKeys, groupKey)
                                },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (groupExpanded) "Collapse" else "Expand",
                                modifier = Modifier
                                    .padding(start = 32.dp, end = 4.dp)
                                    .graphicsLayer { rotationZ = subChevronRotation },
                            )
                            Text(
                                formatEnumLabel(group),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
