package com.lsing.timego.ui.theme

/*
 * ENGINE-ROOM GAUGE PANEL (2026-08-26) — replaces Night Training Console / Backlit.
 *
 * THESIS: TimeGo's numbers are read like an instrument, not decorated like a stat card; the
 * category default this refuses is tonal-card dark mode with a neon accent.
 * OWN-WORLD: riveted gunmetal steel panels (LedgerSurface* / NightDeckLow/High), brass instrument
 * trim as the one warm metal (Brass/BrassDim/BrassHighlight, reserved for controls and active
 * state, never a flat fill), JetBrains Mono for every logged figure, machined near-sharp corners
 * (TimeGoShapes), real cast shadow plus a brass-lit top bezel instead of hairline-only depth.
 * STORY: the lifter reads their PR, target, and progress the way they'd read an instrument panel
 * mid-set — at a glance, trustworthy, never pretending to be smarter than the deterministic rule
 * underneath it.
 * FIRST VIEWPORT: Progress → Personal Records card — a riveted hero panel, brass bezel, real
 * cast shadow. The literal needle-gauge dial widget was tried and cut (rendered wrong, and read
 * as a nonsensical "weight scale" on duration-based hold PRs) — the panel/brass/motion system
 * stands without it; DialFace/DialInk/DialNeedle tokens are retired, not carried forward.
 * FORM: build candidate 6 of 7 grounded gym-world directions was assigned by the concept-seed
 * roll; the user chose their own top-ranked candidate (this one) instead, a legitimate outcome.
 * Seed key cec5980f.
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the
 * verdict, and DESIGN.md.
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
    surfaceContainer = NightDeckLow,
    surfaceContainerHigh = NightDeckHigh,
    surfaceContainerHighest = Color(0xFF2E373B),
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
