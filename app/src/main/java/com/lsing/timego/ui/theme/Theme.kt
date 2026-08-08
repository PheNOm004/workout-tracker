package com.lsing.timego.ui.theme

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
