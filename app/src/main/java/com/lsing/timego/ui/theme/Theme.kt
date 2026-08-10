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
    primary = LedgerAccent,
    onPrimary = LedgerOnAccent,
    primaryContainer = LedgerAccentContainerDark,
    onPrimaryContainer = LedgerOnAccentContainerDark,
    inversePrimary = LedgerAccentDim,
    secondary = LedgerSecondary,
    onSecondary = LedgerOnSecondary,
    secondaryContainer = LedgerSecondaryContainerDark,
    onSecondaryContainer = LedgerOnSecondaryContainerDark,
    tertiary = LedgerTertiary,
    onTertiary = LedgerOnTertiary,
    tertiaryContainer = LedgerTertiaryContainerDark,
    onTertiaryContainer = LedgerOnTertiaryContainerDark,
    background = LedgerBackgroundDark,
    onBackground = LedgerOnSurfaceDark,
    surface = LedgerSurfaceDark,
    onSurface = LedgerOnSurfaceDark,
    surfaceVariant = LedgerSurfaceVariantDark,
    onSurfaceVariant = LedgerOnSurfaceVariantDark,
    surfaceTint = LedgerAccent,
    inverseSurface = LedgerOnSurfaceDark,
    inverseOnSurface = LedgerBackgroundDark,
    surfaceDim = LedgerSurfaceDimDark,
    surfaceBright = LedgerSurfaceBrightDark,
    surfaceContainerLowest = LedgerSurfaceContainerLowestDark,
    surfaceContainerLow = LedgerSurfaceContainerLowDark,
    surfaceContainer = LedgerSurfaceContainerDark,
    surfaceContainerHigh = LedgerSurfaceContainerHighDark,
    surfaceContainerHighest = LedgerSurfaceContainerHighestDark,
    outline = LedgerOutlineDark,
    outlineVariant = LedgerOutlineVariantDark,
    error = LedgerError,
    onError = LedgerOnError,
    errorContainer = LedgerErrorContainerDark,
    onErrorContainer = LedgerOnErrorContainerDark,
    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = LedgerAccentDim,
    onPrimary = Color.White,
    primaryContainer = LedgerAccentContainerLight,
    onPrimaryContainer = LedgerOnAccentContainerLight,
    inversePrimary = LedgerAccent,
    secondary = LedgerSecondary,
    onSecondary = LedgerOnSecondary,
    secondaryContainer = LedgerSecondaryContainerLight,
    onSecondaryContainer = LedgerOnSecondaryContainerLight,
    tertiary = LedgerTertiaryDim,
    onTertiary = LedgerOnTertiary,
    tertiaryContainer = LedgerTertiaryContainerLight,
    onTertiaryContainer = LedgerOnTertiaryContainerLight,
    background = LedgerBackgroundLight,
    onBackground = LedgerOnSurfaceLight,
    surface = LedgerSurfaceLight,
    onSurface = LedgerOnSurfaceLight,
    surfaceVariant = LedgerSurfaceVariantLight,
    onSurfaceVariant = LedgerOnSurfaceVariantLight,
    surfaceTint = LedgerAccentDim,
    inverseSurface = LedgerOnSurfaceLight,
    inverseOnSurface = LedgerBackgroundLight,
    surfaceDim = LedgerSurfaceDimLight,
    surfaceBright = LedgerSurfaceBrightLight,
    surfaceContainerLowest = LedgerSurfaceContainerLowestLight,
    surfaceContainerLow = LedgerSurfaceContainerLowLight,
    surfaceContainer = LedgerSurfaceContainerLight,
    surfaceContainerHigh = LedgerSurfaceContainerHighLight,
    surfaceContainerHighest = LedgerSurfaceContainerHighestLight,
    outline = LedgerOutlineLight,
    outlineVariant = LedgerOutlineVariantLight,
    error = LedgerError,
    onError = LedgerOnError,
    errorContainer = LedgerErrorContainerLight,
    onErrorContainer = LedgerOnErrorContainerLight,
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
