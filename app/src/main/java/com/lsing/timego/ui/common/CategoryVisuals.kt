package com.lsing.timego.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.ui.theme.NightAmber
import com.lsing.timego.ui.theme.NightMint
import com.lsing.timego.ui.theme.NightViolet

/** Icon and semantic accent shown per [ExerciseCategory]. */
data class CategoryVisual(val icon: ImageVector, val accent: Color)

/** Raw-string overload for callers holding an Exercise.category straight out of Room. An
 *  unrecognised value falls back to STRENGTH rather than throwing: ExerciseCategory.valueOf in
 *  a composable turns one bad row into a crash on every render of the exercise list. */
@Composable
fun categoryVisual(rawCategory: String): CategoryVisual =
    categoryVisual(
        ExerciseCategory.entries.firstOrNull { it.name == rawCategory } ?: ExerciseCategory.STRENGTH,
    )

@Composable
fun categoryVisual(category: ExerciseCategory): CategoryVisual {
    val icon = when (category) {
        ExerciseCategory.STRENGTH -> Icons.Filled.FitnessCenter
        ExerciseCategory.CALISTHENICS -> Icons.Filled.Accessibility
        ExerciseCategory.CARDIO -> Icons.Filled.MonitorHeart
        ExerciseCategory.WARMUP -> Icons.Filled.Whatshot
    }
    val accent = when (category) {
        ExerciseCategory.STRENGTH -> MaterialTheme.colorScheme.primary
        ExerciseCategory.CALISTHENICS -> NightMint
        ExerciseCategory.CARDIO -> NightViolet
        ExerciseCategory.WARMUP -> NightAmber
    }
    return CategoryVisual(icon, accent)
}
