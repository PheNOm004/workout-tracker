package com.lsing.timego.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// extraLarge is Material3's default shape for large surfaces like ModalBottomSheet's top corners --
// keep it a real corner radius (28dp), not a 50%-pill shape, or every bottom sheet in the app gets
// an oversized dome that clips content near the top edge.
val TimeGoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
