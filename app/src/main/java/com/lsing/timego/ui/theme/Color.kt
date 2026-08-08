package com.lsing.timego.ui.theme

import androidx.compose.ui.graphics.Color

// "Onyx" palette — TimeGo's cool true-black identity. Chrome (bars/surfaces/text) only.
// Per-habit heatmap colors stay independently user-chosen (see HabitColorPalette below).

// Dark theme: near-black, not neutral/blue-black.
val OnyxBackgroundDark = Color(0xFF0A0A0C)
val OnyxSurfaceDark = Color(0xFF121216)
val OnyxSurfaceVariantDark = Color(0xFF1D1D24)
val OnyxOnSurfaceDark = Color(0xFFE7E9F2)
val OnyxOnSurfaceVariantDark = Color(0xFFB4B7C6)
val OnyxOutlineDark = Color(0xFF6E7185)
val OnyxOutlineVariantDark = Color(0xFF2C2D36)

// Elevation in the Onyx system is expressed as a cool neutral-gray tonal ramp. Every
// `surfaceContainer*` role must be set explicitly: Material3 components (NavigationBar,
// ModalBottomSheet, AlertDialog, Card) resolve their container from this family, and any role left
// unset falls back to the baseline violet-grey palette -- the same lavender-fallback risk the prior
// Ember palette had to guard against (see Theme.kt's matching comment).
val OnyxSurfaceDimDark = Color(0xFF08080A)
val OnyxSurfaceBrightDark = Color(0xFF2E2E37)
val OnyxSurfaceContainerLowestDark = Color(0xFF060608)
val OnyxSurfaceContainerLowDark = Color(0xFF15151A)
val OnyxSurfaceContainerDark = Color(0xFF19191F)
val OnyxSurfaceContainerHighDark = Color(0xFF232329)
val OnyxSurfaceContainerHighestDark = Color(0xFF2D2D35)

// Light theme: cool off-white/light-gray, not stark white.
val OnyxBackgroundLight = Color(0xFFF4F5F9)
val OnyxSurfaceLight = Color(0xFFFCFCFE)
val OnyxSurfaceVariantLight = Color(0xFFE3E5EF)
val OnyxOnSurfaceLight = Color(0xFF15161B)
val OnyxOnSurfaceVariantLight = Color(0xFF464855)
val OnyxOutlineLight = Color(0xFF767A8C)
val OnyxOutlineVariantLight = Color(0xFFC8CBDA)

val OnyxSurfaceDimLight = Color(0xFFD9DBE6)
val OnyxSurfaceBrightLight = Color(0xFFFCFCFE)
val OnyxSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val OnyxSurfaceContainerLowLight = Color(0xFFF7F8FC)
val OnyxSurfaceContainerLight = Color(0xFFF0F1F8)
val OnyxSurfaceContainerHighLight = Color(0xFFE9EBF3)
val OnyxSurfaceContainerHighestLight = Color(0xFFE2E4EE)

// Accents, shared across light/dark (adjusted for contrast where needed).
val OnyxPrimary = Color(0xFF5B8DEF)
val OnyxPrimaryDim = Color(0xFF3F6BC4)
val OnyxOnPrimary = Color(0xFF00214C)
val OnyxSecondary = Color(0xFF4C9BE8)
val OnyxOnSecondary = Color(0xFFFFFFFF)
val OnyxTertiary = Color(0xFF3F9CB0)
val OnyxTertiaryDim = Color(0xFF2C7A8C)
val OnyxOnTertiary = Color(0xFFFFFFFF)
val OnyxError = Color(0xFFCF4A3D)
val OnyxOnError = Color(0xFFFFFFFF)

// Tonal containers — cool-shifted so filled chips, the nav indicator and assist surfaces stay onyx.
val OnyxPrimaryContainerDark = Color(0xFF17386F)
val OnyxOnPrimaryContainerDark = Color(0xFFD3E2FF)
val OnyxSecondaryContainerDark = Color(0xFF12395F)
val OnyxOnSecondaryContainerDark = Color(0xFFD1E7FF)
val OnyxTertiaryContainerDark = Color(0xFF10424B)
val OnyxOnTertiaryContainerDark = Color(0xFFB7E9F0)
val OnyxErrorContainerDark = Color(0xFF5C1A14)
val OnyxOnErrorContainerDark = Color(0xFFFFDAD5)

val OnyxPrimaryContainerLight = Color(0xFFD3E2FF)
val OnyxOnPrimaryContainerLight = Color(0xFF001B3D)
val OnyxSecondaryContainerLight = Color(0xFFD1E7FF)
val OnyxOnSecondaryContainerLight = Color(0xFF001C38)
val OnyxTertiaryContainerLight = Color(0xFFB7E9F0)
val OnyxOnTertiaryContainerLight = Color(0xFF00202A)
val OnyxErrorContainerLight = Color(0xFFFFDAD5)
val OnyxOnErrorContainerLight = Color(0xFF410300)

/** Curated default habit-color presets — a coordinated ember family instead of generic primaries.
 *  This is the short row shown inline on the Add/Edit screen; the full palette lives in
 *  [HabitColorPalette]. Hex strings are uppercase `#RRGGBB` to match what the custom picker emits,
 *  so `colorHex !in presets` correctly identifies a genuinely custom color. */
val HabitColorPresets = listOf(
    "#FF7A3D", // Ember orange
    "#E4572E", // Molten red
    "#F3B23E", // Gold
    "#3F9C93", // Ash teal
    "#8E4585", // Deep plum
)

/** A named swatch in the extended habit palette. */
data class HabitSwatch(val name: String, val hex: String)

/** A tonal family: one hue, three steps (soft / core / deep). */
data class HabitSwatchFamily(val name: String, val swatches: List<HabitSwatch>)

/**
 * The full habit palette offered by the color picker.
 *
 * Deliberately *not* an arbitrary rainbow. Every entry is held inside one saturation/lightness band
 * (roughly S 45-80%, L 45-70%) so that any two habits picked at random still sit together on the
 * Calendar day cells and the Today list, where several habit colors are shown side by side. Hues are
 * spaced evenly around the wheel, and each hue offers a soft / core / deep step so a user with many
 * habits can differentiate without reaching outside the band.
 */
val HabitColorPalette: List<HabitSwatchFamily> = listOf(
    HabitSwatchFamily("Ember", listOf(
        HabitSwatch("Ember light", "#FFA170"),
        HabitSwatch("Ember", "#FF7A3D"),
        HabitSwatch("Ember deep", "#C6551F"),
    )),
    HabitSwatchFamily("Molten", listOf(
        HabitSwatch("Molten light", "#F0846A"),
        HabitSwatch("Molten", "#E4572E"),
        HabitSwatch("Molten deep", "#B03A18"),
    )),
    HabitSwatchFamily("Clay", listOf(
        HabitSwatch("Clay light", "#D89A7E"),
        HabitSwatch("Clay", "#BC6F4C"),
        HabitSwatch("Clay deep", "#8F4E2F"),
    )),
    HabitSwatchFamily("Gold", listOf(
        HabitSwatch("Gold light", "#F7CC72"),
        HabitSwatch("Gold", "#F3B23E"),
        HabitSwatch("Gold deep", "#C58A1E"),
    )),
    HabitSwatchFamily("Olive", listOf(
        HabitSwatch("Olive light", "#B6C077"),
        HabitSwatch("Olive", "#93A24C"),
        HabitSwatch("Olive deep", "#6C7A32"),
    )),
    HabitSwatchFamily("Moss", listOf(
        HabitSwatch("Moss light", "#8CC08C"),
        HabitSwatch("Moss", "#5FA063"),
        HabitSwatch("Moss deep", "#3F7845"),
    )),
    HabitSwatchFamily("Ash teal", listOf(
        HabitSwatch("Teal light", "#71C0B7"),
        HabitSwatch("Teal", "#3F9C93"),
        HabitSwatch("Teal deep", "#2A736C"),
    )),
    HabitSwatchFamily("Slate blue", listOf(
        HabitSwatch("Slate light", "#7FAAC9"),
        HabitSwatch("Slate", "#4E82A6"),
        HabitSwatch("Slate deep", "#355F7D"),
    )),
    HabitSwatchFamily("Indigo", listOf(
        HabitSwatch("Indigo light", "#8B92CC"),
        HabitSwatch("Indigo", "#5C64A8"),
        HabitSwatch("Indigo deep", "#414780"),
    )),
    HabitSwatchFamily("Plum", listOf(
        HabitSwatch("Plum light", "#B573AC"),
        HabitSwatch("Plum", "#8E4585"),
        HabitSwatch("Plum deep", "#6B3163"),
    )),
    HabitSwatchFamily("Rose", listOf(
        HabitSwatch("Rose light", "#DE8A9C"),
        HabitSwatch("Rose", "#C25A72"),
        HabitSwatch("Rose deep", "#973F55"),
    )),
    HabitSwatchFamily("Stone", listOf(
        HabitSwatch("Stone light", "#B5A794"),
        HabitSwatch("Stone", "#8C7C68"),
        HabitSwatch("Stone deep", "#655847"),
    )),
)

/** Flat lookup of every palette hex, uppercase `#RRGGBB`. */
val HabitPaletteHexes: List<String> = HabitColorPalette.flatMap { family -> family.swatches.map { it.hex } }
