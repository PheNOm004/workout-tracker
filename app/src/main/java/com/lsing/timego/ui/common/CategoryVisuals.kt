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

/** Icon shown per [ExerciseCategory] so the exercise list is scannable at a glance -- category is
 *  now carried by icon shape alone, not a per-category hue: the Training Ledger direction commits
 *  to one accent (the red margin rule), so every category icon shares the same neutral tone and
 *  [accent] is only ever the red brand accent, applied by the caller to the active/expanded row
 *  (see LogScreen's ExerciseCard), never as a permanent per-category color. */
data class CategoryVisual(val icon: ImageVector, val accent: Color)

@Composable
fun categoryVisual(category: ExerciseCategory): CategoryVisual {
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val icon = when (category) {
        ExerciseCategory.STRENGTH -> Icons.Filled.FitnessCenter
        ExerciseCategory.CALISTHENICS -> Icons.Filled.Accessibility
        ExerciseCategory.CARDIO -> Icons.Filled.MonitorHeart
        ExerciseCategory.WARMUP -> Icons.Filled.Whatshot
    }
    return CategoryVisual(icon, neutral)
}
