package com.lsing.timego.ui.common

import com.lsing.timego.data.MuscleGroup
import androidx.compose.ui.graphics.lerp
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
import com.lsing.timego.ui.theme.PlateChest
import com.lsing.timego.ui.theme.PlateGreen
import com.lsing.timego.ui.theme.PlateSteel
import com.lsing.timego.ui.theme.PlateYellow

/** The plate colour that identifies a body region everywhere it appears: chest pink, back blue, shoulders
 *  yellow, legs green, arms white, core and anything else steel. */
@Composable
fun regionColor(label: String): Color = when (label) {
    SessionDisplayRegion.CHEST.label -> PlateChest
    SessionDisplayRegion.BACK.label -> PlateBlue
    SessionDisplayRegion.SHOULDERS.label -> PlateYellow
    SessionDisplayRegion.LEGS.label -> PlateGreen
    SessionDisplayRegion.ARMS.label -> PlateArms
    else -> PlateSteel
}

/** Lightness step of a muscle within its region colour: sub-groups share the region's hue and differ in shade
 *  (lats vs upper back, biceps vs triceps vs forearms). Positive lightens, negative darkens; for arms (a neutral)
 *  the step fades toward the background instead. */
private val SUBGROUP_SHADE = mapOf(
    MuscleGroup.CHEST.name to 0f,
    MuscleGroup.LATS.name to 0f, MuscleGroup.UPPER_BACK.name to 0.3f, MuscleGroup.TRAPS.name to -0.25f, MuscleGroup.LOWER_BACK.name to -0.45f,
    MuscleGroup.FRONT_DELTS.name to 0f, MuscleGroup.SIDE_DELTS.name to 0.3f, MuscleGroup.REAR_DELTS.name to -0.3f,
    MuscleGroup.BICEPS.name to 0f, MuscleGroup.TRICEPS.name to -0.3f, MuscleGroup.FOREARMS.name to -0.55f,
    MuscleGroup.QUADS.name to 0f, MuscleGroup.HAMSTRINGS.name to 0.3f, MuscleGroup.GLUTES.name to -0.3f,
    MuscleGroup.ADDUCTORS.name to 0.5f, MuscleGroup.CALVES.name to -0.5f,
    MuscleGroup.ABS.name to 0f, MuscleGroup.OBLIQUES.name to -0.35f,
)

/** The region colour of [group], shaded by its sub-group so neighbouring muscles stay distinguishable. */
@Composable
fun muscleGroupColor(group: String, hasBackMuscles: Boolean = false): Color {
    val region = sessionDisplayRegion(group, hasBackMuscles) ?: return PlateSteel
    val base = regionColor(region.label)
    val shade = SUBGROUP_SHADE[group] ?: 0f
    return when {
        shade == 0f -> base
        region == SessionDisplayRegion.ARMS -> lerp(base, MaterialTheme.colorScheme.surface, -shade)
        shade > 0f -> lerp(base, Color.White, shade)
        else -> lerp(base, Color.Black, -shade)
    }
}

/** The distinct body regions a set of muscle groups touches, in the usual chest-to-core order. */
fun regionLabelsFor(groups: Collection<String>): List<String> {
    val hasBack = groups.any {
        it == MuscleGroup.LATS.name || it == MuscleGroup.UPPER_BACK.name || it == MuscleGroup.LOWER_BACK.name || it == MuscleGroup.TRAPS.name
    }
    return groups.mapNotNull { sessionDisplayRegion(it, hasBack) }.distinct().sortedBy { it.ordinal }.map { it.label }
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
