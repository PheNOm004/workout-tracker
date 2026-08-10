package com.lsing.timego.ui.theme

/*
 * IMPECCABLE DIRECTION CONTRACT (2026-08-11, redesign, seed ed48a011, MY PICK card)
 *
 * THESIS: TimeGo is the lifter's own logbook, not a fitness-brand dashboard — it refuses
 * the category default of vibrant orange/green motivational-app chrome (explicitly declined
 * once already) and the safe Material-defaults-with-icons pass that still read as generic.
 *
 * OWN-WORLD: A serious lifter's paper training ledger, rendered dark-first rather than the
 * cream/parchment default: graphite graph-paper grid on a near-black page (dark) or bone
 * paper (light, not the primary mode), tabular monospace numerals for every logged value,
 * one committed red margin-rule accent (brick/ink red, not neon) carrying state and primary
 * action, ruled horizontal lines standing in for card dividers.
 *
 * STORY: The lifter opens the app mid-set, sees today's page with suggested targets already
 * penciled in, logs the set, and later flips back through past pages (Progress) to see the
 * curve/PRs/heatmap as a ledger of what they've done — trustworthy, inspectable, personal.
 *
 * FIRST VIEWPORT: Log screen renders as a ruled ledger page — a graph-paper grid ground,
 * today's date as a ledger header rule, exercise rows as ruled lines with tabular-numeral
 * suggestion values pre-filled, the red rule marking the active/expanded row.
 *
 * FORM: Training Ledger — MY PICK card, chosen over the assigned direction (Street Rig
 * Signage, seed ed48a011, candidate 4 of 7 grounded worlds ordered by resonance).
 *
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review,
 * the verdict, and DESIGN.md.
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every Material3 color role is filled in explicitly. Roles left unspecified fall back to the
// baseline (violet-grey) palette, and those defaults surface in components the app never styles
// directly — NavigationBar's container and its selected-item pill, ModalBottomSheet, AlertDialog.
private val DarkColorScheme = darkColorScheme(
    primary = OnyxPrimary,
    onPrimary = OnyxOnPrimary,
    primaryContainer = OnyxPrimaryContainerDark,
    onPrimaryContainer = OnyxOnPrimaryContainerDark,
    inversePrimary = OnyxPrimaryDim,
    secondary = OnyxSecondary,
    onSecondary = OnyxOnSecondary,
    secondaryContainer = OnyxSecondaryContainerDark,
    onSecondaryContainer = OnyxOnSecondaryContainerDark,
    tertiary = OnyxTertiary,
    onTertiary = OnyxOnTertiary,
    tertiaryContainer = OnyxTertiaryContainerDark,
    onTertiaryContainer = OnyxOnTertiaryContainerDark,
    background = OnyxBackgroundDark,
    onBackground = OnyxOnSurfaceDark,
    surface = OnyxSurfaceDark,
    onSurface = OnyxOnSurfaceDark,
    surfaceVariant = OnyxSurfaceVariantDark,
    onSurfaceVariant = OnyxOnSurfaceVariantDark,
    surfaceTint = OnyxPrimary,
    inverseSurface = OnyxOnSurfaceDark,
    inverseOnSurface = OnyxBackgroundDark,
    surfaceDim = OnyxSurfaceDimDark,
    surfaceBright = OnyxSurfaceBrightDark,
    surfaceContainerLowest = OnyxSurfaceContainerLowestDark,
    surfaceContainerLow = OnyxSurfaceContainerLowDark,
    surfaceContainer = OnyxSurfaceContainerDark,
    surfaceContainerHigh = OnyxSurfaceContainerHighDark,
    surfaceContainerHighest = OnyxSurfaceContainerHighestDark,
    outline = OnyxOutlineDark,
    outlineVariant = OnyxOutlineVariantDark,
    error = OnyxError,
    onError = OnyxOnError,
    errorContainer = OnyxErrorContainerDark,
    onErrorContainer = OnyxOnErrorContainerDark,
    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = OnyxPrimaryDim,
    onPrimary = Color.White,
    primaryContainer = OnyxPrimaryContainerLight,
    onPrimaryContainer = OnyxOnPrimaryContainerLight,
    inversePrimary = OnyxPrimary,
    secondary = OnyxSecondary,
    onSecondary = OnyxOnSecondary,
    secondaryContainer = OnyxSecondaryContainerLight,
    onSecondaryContainer = OnyxOnSecondaryContainerLight,
    tertiary = OnyxTertiaryDim,
    onTertiary = OnyxOnTertiary,
    tertiaryContainer = OnyxTertiaryContainerLight,
    onTertiaryContainer = OnyxOnTertiaryContainerLight,
    background = OnyxBackgroundLight,
    onBackground = OnyxOnSurfaceLight,
    surface = OnyxSurfaceLight,
    onSurface = OnyxOnSurfaceLight,
    surfaceVariant = OnyxSurfaceVariantLight,
    onSurfaceVariant = OnyxOnSurfaceVariantLight,
    surfaceTint = OnyxPrimaryDim,
    inverseSurface = OnyxOnSurfaceLight,
    inverseOnSurface = OnyxBackgroundLight,
    surfaceDim = OnyxSurfaceDimLight,
    surfaceBright = OnyxSurfaceBrightLight,
    surfaceContainerLowest = OnyxSurfaceContainerLowestLight,
    surfaceContainerLow = OnyxSurfaceContainerLowLight,
    surfaceContainer = OnyxSurfaceContainerLight,
    surfaceContainerHigh = OnyxSurfaceContainerHighLight,
    surfaceContainerHighest = OnyxSurfaceContainerHighestLight,
    outline = OnyxOutlineLight,
    outlineVariant = OnyxOutlineVariantLight,
    error = OnyxError,
    onError = OnyxOnError,
    errorContainer = OnyxErrorContainerLight,
    onErrorContainer = OnyxOnErrorContainerLight,
    scrim = Color.Black,
)

@Composable
fun TimeGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Deliberately no dynamic (Material You / wallpaper-derived) color: TimeGo has its own
    // "onyx" identity rather than borrowing whatever palette the phone's wallpaper produces.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TimeGoShapes,
        content = content,
    )
}
