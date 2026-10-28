package com.lsing.timego.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * PLATE COLOURS. The accents are calibrated barbell-plate colours (red, blue, yellow, green, white,
 * steel) on warm rubber-black (dark) and chalk (light). Neutrals are tinted warm instead of flat grey.
 * Every value is an OKLCH ramp step; text/background pairs are verified at >= 4.5:1 (graphics >= 3:1).
 *
 * Red is the single action colour (anything tappable). Plate colours otherwise encode meaning:
 * chest violet, back blue, shoulders yellow, legs green, arms white, core steel.
 */

// ---- Dark: rubber floor -------------------------------------------------------------------------

private val DarkBackground = Color(0xFF0E0F12)
private val DarkSurface = Color(0xFF151619)
private val DarkContainerLow = Color(0xFF121417)
private val DarkContainer = Color(0xFF1A1C1F)
private val DarkContainerHigh = Color(0xFF232529)
private val DarkContainerHighest = Color(0xFF2D2F34)
private val DarkOnSurface = Color(0xFFF0F2F4)
private val DarkOnSurfaceVariant = Color(0xFFB5B7BD)
private val DarkOutline = Color(0xFF72747B)
private val DarkOutlineVariant = Color(0xFF3A3D43)

private val DarkRed = Color(0xFFFB6C3E)
private val DarkOnRed = Color(0xFF1D0D08)
private val DarkRedContainer = Color(0xFF561F11)
private val DarkOnRedContainer = Color(0xFFFCDECD)
private val DarkBlue = Color(0xFF65A7FA)
private val DarkOnBlue = Color(0xFF051223)
private val DarkBlueContainer = Color(0xFF183356)
private val DarkOnBlueContainer = Color(0xFFD6E7F8)
private val DarkYellow = Color(0xFFF8CC2F)
private val DarkOnYellow = Color(0xFF1D1500)
private val DarkYellowContainer = Color(0xFF453600)
private val DarkOnYellowContainer = Color(0xFFF2E8C3)
private val DarkGreen = Color(0xFF3EC87D)
private val DarkSteel = Color(0xFFA7AFB5)
private val DarkError = Color(0xFFFD7273)
private val DarkOnError = Color(0xFF200909)
private val DarkErrorContainer = Color(0xFF581B1B)
private val DarkOnErrorContainer = Color(0xFFFFDBD8)

// ---- Light: chalk ---------------------------------------------------------------------------------

private val LightBackground = Color(0xFFEFF0F3)
private val LightSurface = Color(0xFFF9FAFC)
private val LightContainerLow = Color(0xFFF4F5F7)
private val LightContainer = Color(0xFFE9EBEF)
private val LightContainerHigh = Color(0xFFE1E3E7)
private val LightContainerHighest = Color(0xFFD8DBDF)
private val LightOnSurface = Color(0xFF14161A)
private val LightOnSurfaceVariant = Color(0xFF4A4D54)
private val LightOutline = Color(0xFF6B6F76)
private val LightOutlineVariant = Color(0xFFCBCED3)

private val LightRed = Color(0xFFBD2900)
private val LightOnRed = Color(0xFFFFFBF9)
private val LightRedContainer = Color(0xFFFFDBCA)
private val LightOnRedContainer = Color(0xFF5B1908)
private val LightBlue = Color(0xFF145EC1)
private val LightOnBlue = Color(0xFFFAFCFF)
private val LightBlueContainer = Color(0xFFD3E7FF)
private val LightOnBlueContainer = Color(0xFF062B5F)
/** Light-mode yellow is split: [LightYellowFill] is the bright plate yellow, used only behind dark ink
 *  text; [LightAmber] is the darker gold for small graphics (dots, bars, lines) that need >= 3:1. */
private val LightYellowFill = Color(0xFFF8CC2F)
private val LightAmber = Color(0xFFB37900)
private val LightOnYellow = Color(0xFF1A1000)
private val LightYellowContainer = Color(0xFFF8E8AB)
private val LightOnYellowContainer = Color(0xFF452E00)
private val LightGreen = Color(0xFF007840)
private val LightSteel = Color(0xFF5D646A)
private val LightError = Color(0xFFB7191C)
private val LightOnError = Color(0xFFFFFBFA)
private val LightErrorContainer = Color(0xFFFFD8D4)
private val LightOnErrorContainer = Color(0xFF620A0B)

/** Everything beyond the Material roles: the plate colours as meaning, tonal card steps, glass. */
@Immutable
class TimeGoPalette(
    val red: Color,
    /** Chest. Distinct from [red], which only ever means an action. */
    val chest: Color,
    val blue: Color,
    /** Yellow for small graphics (dots, bars, lines, text-on-dark). Darker gold in light mode. */
    val yellow: Color,
    /** Bright plate yellow, only ever a fill behind [onYellow] text. */
    val yellowFill: Color,
    val onYellow: Color,
    val yellowContainer: Color,
    val onYellowContainer: Color,
    val green: Color,
    val onGreen: Color,
    val steel: Color,
    /** The white plate: arms. Near-white on dark, deep ink on light. */
    val arms: Color,
    /** Resting card fill and the one step above it (sheets, selected rows). */
    val card: Color,
    val raised: Color,
    val hairline: Color,
    /** The single translucent surface (44%) shared by every frosted element: dock, rest bar, selector, sheets, dialogs. */
    val glass: Color,
    val glassEdge: Color,
    val scrim: Color,
)

internal val DarkPalette = TimeGoPalette(
    red = DarkRed,
    chest = Color(0xFFBB8AEF),
    blue = DarkBlue,
    yellow = DarkYellow,
    yellowFill = DarkYellow,
    onYellow = DarkOnYellow,
    yellowContainer = DarkYellowContainer,
    onYellowContainer = DarkOnYellowContainer,
    green = DarkGreen,
    onGreen = Color(0xFF021709),
    steel = DarkSteel,
    arms = DarkOnSurface,
    card = DarkContainer,
    raised = DarkContainerHigh,
    hairline = Color(0x24F0F2F4),
    glass = Color(0x701A1C1F),
    glassEdge = Color(0x24F0F2F4),
    scrim = Color(0x59000000),
)

internal val LightPalette = TimeGoPalette(
    red = LightRed,
    chest = Color(0xFF7F36BC),
    blue = LightBlue,
    yellow = LightAmber,
    yellowFill = LightYellowFill,
    onYellow = LightOnYellow,
    yellowContainer = LightYellowContainer,
    onYellowContainer = LightOnYellowContainer,
    green = LightGreen,
    onGreen = Color(0xFFF9FDFA),
    steel = LightSteel,
    arms = Color(0xFF34373D),
    card = LightSurface,
    raised = LightContainerLow,
    hairline = Color(0x2614161A),
    glass = Color(0x70F9FAFC),
    glassEdge = Color(0x1F14161A),
    scrim = Color(0x2E14161A),
)

/** The active palette; provided by [TimeGoTheme]. Static: it changes only on a light/dark switch. */
val LocalTimeGoPalette = staticCompositionLocalOf { DarkPalette }

/** True while the dark palette is active; provided by [TimeGoTheme]. */
val LocalTimeGoDark = staticCompositionLocalOf { true }

val PlateRed: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.red
val PlateChest: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.chest
val PlateBlue: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.blue
val PlateYellow: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.yellow
val PlateGreen: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.green
val OnPlateGreen: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.onGreen
val PlateSteel: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.steel
val PlateArms: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.arms
val CardFill: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.card
val RaisedFill: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.raised
val Hairline: Color @Composable @ReadOnlyComposable get() = LocalTimeGoPalette.current.hairline

val StatusProgressing: Color @Composable @ReadOnlyComposable get() = PlateGreen
val StatusPlateauing: Color @Composable @ReadOnlyComposable get() = PlateYellow

// Every Material3 role is set explicitly: unset roles fall back to the baseline violet palette and leak
// into components the app never styles directly (NavigationBar, ModalBottomSheet, AlertDialog).
internal val DarkScheme = darkColorScheme(
    primary = DarkRed, onPrimary = DarkOnRed, primaryContainer = DarkRedContainer, onPrimaryContainer = DarkOnRedContainer,
    inversePrimary = LightRed,
    secondary = DarkBlue, onSecondary = DarkOnBlue, secondaryContainer = DarkBlueContainer, onSecondaryContainer = DarkOnBlueContainer,
    tertiary = DarkYellow, onTertiary = DarkOnYellow, tertiaryContainer = DarkYellowContainer, onTertiaryContainer = DarkOnYellowContainer,
    background = DarkBackground, onBackground = DarkOnSurface,
    surface = DarkSurface, onSurface = DarkOnSurface, surfaceVariant = DarkContainerHigh, onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkRed,
    inverseSurface = DarkOnSurface, inverseOnSurface = DarkBackground,
    surfaceDim = DarkBackground, surfaceBright = DarkContainerHighest,
    surfaceContainerLowest = DarkBackground, surfaceContainerLow = DarkContainerLow, surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh, surfaceContainerHighest = DarkContainerHighest,
    outline = DarkOutline, outlineVariant = DarkOutlineVariant,
    error = DarkError, onError = DarkOnError, errorContainer = DarkErrorContainer, onErrorContainer = DarkOnErrorContainer,
    scrim = Color.Black,
)

internal val LightScheme = lightColorScheme(
    primary = LightRed, onPrimary = LightOnRed, primaryContainer = LightRedContainer, onPrimaryContainer = LightOnRedContainer,
    inversePrimary = DarkRed,
    secondary = LightBlue, onSecondary = LightOnBlue, secondaryContainer = LightBlueContainer, onSecondaryContainer = LightOnBlueContainer,
    tertiary = LightAmber, onTertiary = LightOnRed, tertiaryContainer = LightYellowContainer, onTertiaryContainer = LightOnYellowContainer,
    background = LightBackground, onBackground = LightOnSurface,
    surface = LightSurface, onSurface = LightOnSurface, surfaceVariant = LightContainerHigh, onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightRed,
    inverseSurface = LightOnSurface, inverseOnSurface = LightBackground,
    surfaceDim = LightContainerHighest, surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurface, surfaceContainerLow = LightContainerLow, surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh, surfaceContainerHighest = LightContainerHighest,
    outline = LightOutline, outlineVariant = LightOutlineVariant,
    error = LightError, onError = LightOnError, errorContainer = LightErrorContainer, onErrorContainer = LightOnErrorContainer,
    scrim = Color.Black,
)
