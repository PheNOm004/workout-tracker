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
import com.lsing.timego.ui.theme.OnyxPrimary
import com.lsing.timego.ui.theme.OnyxSecondary
import com.lsing.timego.ui.theme.OnyxTertiary

/** Icon + accent color shown per [ExerciseCategory] so the exercise list is scannable at a
 *  glance instead of a wall of identical rows. WARMUP uses the theme's neutral outline color
 *  (not an Onyx accent) -- deliberately muted, since warmups aren't the main event. */
data class CategoryVisual(val icon: ImageVector, val accent: Color)

@Composable
fun categoryVisual(category: ExerciseCategory): CategoryVisual = when (category) {
    ExerciseCategory.STRENGTH -> CategoryVisual(Icons.Filled.FitnessCenter, OnyxPrimary)
    ExerciseCategory.CALISTHENICS -> CategoryVisual(Icons.Filled.Accessibility, OnyxTertiary)
    ExerciseCategory.CARDIO -> CategoryVisual(Icons.Filled.MonitorHeart, OnyxSecondary)
    ExerciseCategory.WARMUP -> CategoryVisual(Icons.Filled.Whatshot, MaterialTheme.colorScheme.outline)
}
