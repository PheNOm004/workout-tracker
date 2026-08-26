package com.lsing.timego.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Machined panel edges: tight radii read as cut/milled steel plate, not a soft rounded-rectangle
// card. Kept just above sharp (never 0dp) so touch targets still feel intentional, not clipped.
val TimeGoShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
