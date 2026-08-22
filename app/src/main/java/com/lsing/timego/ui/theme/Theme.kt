package com.lsing.timego.ui.theme

/*
 * NIGHT TRAINING CONSOLE — BACKLIT (2026-08-22)
 *
 * A focused console opened in a dark gym: surfaces are machined decks lit from above; data
 * emits light while chrome absorbs it. This deepens the existing Night Training Console rather
 * than replacing it with another identity.
 *
 * 1. Three tonal steps: ground → deck → raised. Rows remain flat within a card.
 * 2. Raised surfaces use a 1dp hairline; hero decks may use a short top sheen. No drop shadows
 *    except the navigation pill and FAB.
 * 3. Glow is limited to active navigation, primary FAB, chart series, and training-pulse moments.
 * 4. Logged numbers use JetBrains Mono; 20sp is standard and 22sp is the hero ceiling. Uppercase
 *    tracked Manrope eyebrows establish hierarchy instead of oversized figures.
 * 5. Charts carry body through fills, reference tags, readable empty states, and history-aware
 *    comparison—not fabricated data.
 * 6. Anatomical muscle art remains TimeGo's richest visual mark.
 * 7. The set-logged/PR pulse is the single motion signature; other motion stays restrained.
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
