package com.lsing.timego.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Near-rectangular scale -- a ruled ledger page reads flat, not soft/bubbly. extraLarge (used by
// ModalBottomSheet's top corners) keeps enough radius that a sheet doesn't dome oddly near the
// top edge, but pulled down hard from the prior 28dp -- see the original comment this replaces.
val TimeGoShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
