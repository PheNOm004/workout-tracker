# TimeGo Training Ledger Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Per this project's established preference, prefer inline execution over subagent-driven for the actual coding tasks — subagent-driven is fine for the mechanical file-touch fan-out if the harness offers it, but the user has previously asked for inline execution to conserve tokens.

**Goal:** Replace TimeGo's Onyx dark palette, Manrope/Fraunces type scale, rounded card chrome, and tween-based motion with the Training Ledger direction — a graphite graph-paper ledger page (dark-first, bone-paper light mode), tabular monospace numerals for every logged value, one brick-red accent, ruled hairline dividers instead of elevated cards, and spring-based motion — across the whole app (Log, Progress, Routines).

**Architecture:** Foundation-first, same pattern as the Aug 10 visual-identity pass: shared tokens in `ui/theme` and `ui/common` change first, then each screen is re-applied against the new tokens. Order: color → type (+ new font asset) → shape → motion → Log → Progress → Routines. This is a pure restyle — no `data/`, `domain/`, or `*ViewModel.kt` changes except the one color-constant file (`MuscleHeatColor.kt`) whose hex literals move to the new palette family (interpolation logic untouched).

**Tech Stack:** Kotlin, Jetpack Compose, Material3. No new Gradle dependency — only a new bundled font file (`res/font/`).

## Global Constraints

- No proactive screenshots — user verifies on-device on their Galaxy S23 Ultra (established project preference).
- Verify with `./gradlew :app:compileDebugKotlin` after each foundation task, `./gradlew assembleDebug` + `./gradlew testDebugUnitTest` after Task 9 and again at the end (Task 15).
- Direction contract lives at the top of `ui/theme/Theme.kt` — do not remove it; it documents this build against the spec (`docs/superpowers/specs/2026-08-11-timego-training-ledger-redesign-design.md`).
- Naming convention: new tokens use a `Ledger` prefix (parallel to the retired `Onyx` prefix) so a future `grep Ledger` finds every token this pass introduced.

---

### Task 1: Source and bundle the ledger monospace font

**Files:**
- Create: `app/src/main/res/font/jetbrains_mono_variable.ttf`

**Interfaces:**
- Produces: a bundled variable font file consumed by Task 4 (`Type.kt`'s `LedgerMonoFamily`).

- [ ] **Step 1: Download JetBrains Mono (variable, Apache-2.0)**

```bash
curl -L -o app/src/main/res/font/jetbrains_mono_variable.ttf \
  https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/variable/JetBrainsMono[wght].ttf
```

If that path 404s (repo layout can change), fetch the current release asset from https://github.com/JetBrains/JetBrainsMono/releases instead — grab the variable-font `.ttf` (not the static-weight family, to match the `FontVariation`-based pattern `ManropeFamily`/`FrauncesFamily` already use in `Type.kt`). Confirm the license file in that release is Apache-2.0 (or OFL — JetBrains Mono ships under OFL 1.1 for the font itself, Apache-2.0 for tooling; either is compatible with bundling in an APK) before committing the binary.

- [ ] **Step 2: Verify the file is a valid variable font**

Run: `file app/src/main/res/font/jetbrains_mono_variable.ttf`
Expected: reports a TrueType/OpenType font. If curl pulled an HTML error page instead (check file size — a real variable TTF is several hundred KB, an error page is a few KB), redo Step 1 against the release-asset URL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/font/jetbrains_mono_variable.ttf
git commit -m "Add JetBrains Mono variable font for the Training Ledger redesign"
```

---

### Task 2: Ledger color tokens

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Color.kt` (full rewrite)

**Interfaces:**
- Produces: `Ledger*` color vals replacing every `Onyx*` val (dark/light background/surface/outline family, `LedgerAccent`/`LedgerOnAccent` replacing primary, status colors `LedgerProgressing`/`LedgerPlateauing`/`LedgerRegressing`) — consumed by Task 3 (`Theme.kt`).
- Removes: `HabitColorPresets`, `HabitSwatch`, `HabitSwatchFamily`, `HabitColorPalette`, `HabitPaletteHexes` (confirmed zero references outside this file).

- [ ] **Step 1: Replace the full contents of `Color.kt`**

```kotlin
package com.lsing.timego.ui.theme

import androidx.compose.ui.graphics.Color

// "Ledger" palette — a lifter's paper training logbook, rendered dark-first. Chrome
// (backgrounds/surfaces/text/rules) only; chart-specific colors (heatmap scale, radar chart)
// live in their own domain/UI files and are re-picked separately within this same family.

// Dark theme (primary mode): warm near-black graphite, not the prior cool blue-black --
// closer to a ledger page under a desk lamp than a phone-screen black.
val LedgerBackgroundDark = Color(0xFF0D0C0A)
val LedgerSurfaceDark = Color(0xFF16140F)
val LedgerSurfaceVariantDark = Color(0xFF211E17)
val LedgerOnSurfaceDark = Color(0xFFEDE8DC)
val LedgerOnSurfaceVariantDark = Color(0xFFB8B2A0)
val LedgerOutlineDark = Color(0xFF7A7461)
val LedgerOutlineVariantDark = Color(0xFF332F25)

// Elevation/tonal ramp for the roles Material3 resolves from surfaceContainer* -- every role is
// set explicitly for the same reason the prior Onyx system set them explicitly: an unset role
// falls back to the baseline violet-grey palette in NavigationBar/ModalBottomSheet/AlertDialog/Card.
val LedgerSurfaceDimDark = Color(0xFF0A0908)
val LedgerSurfaceBrightDark = Color(0xFF322E24)
val LedgerSurfaceContainerLowestDark = Color(0xFF080706)
val LedgerSurfaceContainerLowDark = Color(0xFF19170F)
val LedgerSurfaceContainerDark = Color(0xFF1D1A13)
val LedgerSurfaceContainerHighDark = Color(0xFF27231A)
val LedgerSurfaceContainerHighestDark = Color(0xFF322D22)

// Light theme: real bone/aged-paper ground -- its own tonal pass, not a mechanical invert.
val LedgerBackgroundLight = Color(0xFFF6F1E4)
val LedgerSurfaceLight = Color(0xFFFBF8EF)
val LedgerSurfaceVariantLight = Color(0xFFE9E1CC)
val LedgerOnSurfaceLight = Color(0xFF211E17)
val LedgerOnSurfaceVariantLight = Color(0xFF534E3E)
val LedgerOutlineLight = Color(0xFF847D66)
val LedgerOutlineVariantLight = Color(0xFFD6CCAE)

val LedgerSurfaceDimLight = Color(0xFFE2D9C0)
val LedgerSurfaceBrightLight = Color(0xFFFBF8EF)
val LedgerSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val LedgerSurfaceContainerLowLight = Color(0xFFF8F3E5)
val LedgerSurfaceContainerLight = Color(0xFFF1EADA)
val LedgerSurfaceContainerHighLight = Color(0xFFEBE2CE)
val LedgerSurfaceContainerHighestLight = Color(0xFFE5DBC3)

// The one committed brand accent -- brick/ink red, carries the margin rule, active-row indicator,
// FAB, and primary buttons. Shared across light/dark (contrast-adjusted where Material requires
// an "on" color); no secondary/tertiary brand hues -- both roles collapse to graphite tonal steps
// so the palette stays Restrained (neutrals + one accent).
val LedgerAccent = Color(0xFFB33A2E)
val LedgerAccentDim = Color(0xFF8C2C22)
val LedgerOnAccent = Color(0xFFFFF3EC)
val LedgerSecondary = Color(0xFF8A8367)
val LedgerOnSecondary = Color(0xFF1D1A13)
val LedgerTertiary = Color(0xFF6E6A57)
val LedgerTertiaryDim = Color(0xFF56523F)
val LedgerOnTertiary = Color(0xFFF6F1E4)
val LedgerError = Color(0xFFCF4A3D)
val LedgerOnError = Color(0xFFFFFFFF)

// Tonal containers -- warm-shifted so filled chips, the nav indicator, and assist surfaces stay
// inside the ledger's graphite/paper family instead of Material's default violet-grey.
val LedgerAccentContainerDark = Color(0xFF4A160F)
val LedgerOnAccentContainerDark = Color(0xFFFFDAD2)
val LedgerSecondaryContainerDark = Color(0xFF3A3728)
val LedgerOnSecondaryContainerDark = Color(0xFFE5DFC7)
val LedgerTertiaryContainerDark = Color(0xFF302D20)
val LedgerOnTertiaryContainerDark = Color(0xFFDBD6C0)
val LedgerErrorContainerDark = Color(0xFF5C1A14)
val LedgerOnErrorContainerDark = Color(0xFFFFDAD5)

val LedgerAccentContainerLight = Color(0xFFFFDAD2)
val LedgerOnAccentContainerLight = Color(0xFF3A0E08)
val LedgerSecondaryContainerLight = Color(0xFFE5DFC7)
val LedgerOnSecondaryContainerLight = Color(0xFF201E10)
val LedgerTertiaryContainerLight = Color(0xFFDBD6C0)
val LedgerOnTertiaryContainerLight = Color(0xFF1C1A0F)
val LedgerErrorContainerLight = Color(0xFFFFDAD5)
val LedgerOnErrorContainerLight = Color(0xFF410300)

// Functional status color -- deliberately separate from the brand accent above, so a plateau
// flag never reads as "the app's red button." Used only for PlateauStatus (PROGRESSING /
// PLATEAUING / REGRESSING) and PR-moment highlighting, never for chrome or navigation.
val LedgerProgressing = Color(0xFF5E8C5A)
val LedgerPlateauing = Color(0xFFC98A2E)
val LedgerRegressing = LedgerError
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: fails at this point — `Theme.kt` still references `Onyx*` names that no longer exist. That's expected; Task 3 fixes it. Confirm the failure is *only* unresolved-reference errors in `Theme.kt` (not a syntax error in the new `Color.kt`) by reading the compiler output.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/theme/Color.kt
git commit -m "Replace Onyx color tokens with the Training Ledger palette"
```

(Deliberately committed even though the build is red — `Theme.kt` is the very next task and this keeps each commit to one file's worth of change, matching the project's established per-task commit granularity. If your workflow prefers a green build at every commit, squash Tasks 2+3 into one commit instead.)

---

### Task 3: Wire `Theme.kt` to the Ledger tokens

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Theme.kt` (color-scheme section only; the direction-contract comment block stays untouched)

**Interfaces:**
- Consumes: every `Ledger*` val from Task 2.

- [ ] **Step 1: Replace `DarkColorScheme` and `LightColorScheme`**

Replace the two `private val ...ColorScheme = ...ColorScheme(...)` blocks (currently lines ~39-116, after the direction-contract comment and imports) with:

```kotlin
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
```

Leave the rest of the file (imports, the `TimeGoTheme` composable, the "no dynamic color" comment) exactly as-is — only the two color-scheme vals change.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If any `Onyx*` reference remains unresolved, grep for it: `grep -rn "Onyx" app/src/main/java` and fix the remaining call site (should only be `Color.kt`/`Theme.kt` at this point — Tasks 8–13 handle every screen-level `Onyx*` reference separately since those aren't color-scheme roles).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/theme/Theme.kt
git commit -m "Wire Theme.kt color schemes to the Ledger palette"
```

---

### Task 4: Typography — ledger mono numerals, retire Fraunces

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Type.kt` (full rewrite)

**Interfaces:**
- Produces: `LedgerMonoFamily`, `LedgerFigureValue` (replaces `FrauncesStatValue`), `LedgerFigureEmphasis` (replaces `FrauncesEmphasis`) — consumed by Tasks 9 (Log), 10 (Progress `StatTile`), 12 (Routines routine names).
- Removes: `FrauncesFamily`, `FrauncesStatValue`, `FrauncesEmphasis`, every `FrauncesFamily` reference in the `Typography` val.

- [ ] **Step 1: Replace the full contents of `Type.kt`**

```kotlin
@file:OptIn(ExperimentalTextApi::class)

package com.lsing.timego.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lsing.timego.R

/** Manrope (geometric sans) carries all UI chrome and body text -- legible at small sizes. */
val ManropeFamily = FontFamily(
    Font(
        R.font.manrope_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.manrope_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.manrope_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.manrope_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/** JetBrains Mono (tabular monospace) carries every *logged number* -- weight, reps, seconds,
 *  distance, dates, PR values, chart axis labels. Tabular figures are what make a column of
 *  logged sets actually align like a ledger; this is structural to the direction, not decoration. */
val LedgerMonoFamily = FontFamily(
    Font(
        R.font.jetbrains_mono_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.jetbrains_mono_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.jetbrains_mono_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.jetbrains_mono_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

// Display/headline roles move to LedgerMonoFamily (a ruled ledger's masthead reads as a stamped
// figure, not a warm display serif -- Fraunces is retired, see the redesign spec Section 2).
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = LedgerMonoFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = LedgerMonoFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = LedgerMonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = LedgerMonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

/** Ledger-mono treatment for PR stat-tile values and every other "logged number" moment --
 *  TimeGo's tabular-figure equivalent of the old FrauncesStatValue. */
val LedgerFigureValue = TextStyle(
    fontFamily = LedgerMonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
)

/** Ledger-mono treatment for short identity-bearing labels (routine names) -- replaces
 *  FrauncesEmphasis. */
val LedgerFigureEmphasis = TextStyle(
    fontFamily = LedgerMonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
)
```

- [ ] **Step 2: Delete the Fraunces font asset**

```bash
git rm app/src/main/res/font/fraunces_variable.ttf
```

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: fails — `RoutinesScreen.kt` and `ProgressScreen.kt` still import `FrauncesEmphasis`/`FrauncesStatValue`. Confirm the only errors are those two unresolved imports; Tasks 10 and 12 fix them.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/theme/Type.kt
git commit -m "Add LedgerMonoFamily, retire Fraunces"
```

---

### Task 5: Flatten the shape scale

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Shapes.kt` (full rewrite)

- [ ] **Step 1: Replace the full contents of `Shapes.kt`**

```kotlin
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
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (shape roles are consumed automatically by every Material component through `MaterialTheme.shapes` — no call-site changes needed).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/theme/Shapes.kt
git commit -m "Flatten shape scale for the ledger's ruled-page character"
```

---

### Task 6: Motion — spring-based `AnimatedExpand`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/AnimatedExpand.kt` (full rewrite)

**Interfaces:** unchanged signature (`AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit)`) — every existing call site (`LogScreen.kt`'s three row types, `ExerciseListSections.kt`'s category/muscle-group headers) keeps working with no edits.

- [ ] **Step 1: Replace the full contents of `AnimatedExpand.kt`**

```kotlin
package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared expand/collapse transition for the app's several collapsible sections (exercise log
 *  rows, library category/muscle-group headers) -- spring-based rather than the prior linear
 *  tween, per the Training Ledger direction's motion answer to "feels generic/flat": a page
 *  entry settling into place has weight, not a mechanical ease curve. Exit stays snappier than
 *  enter (higher stiffness, no bounce) so collapsing never feels like it's fighting the user.
 *  Wraps [content] in a Column since AnimatedVisibility needs a single child layout slot. */
@Composable
fun AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        ) + fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = shrinkVertically(
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh)),
    ) {
        Column { content() }
    }
}
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/AnimatedExpand.kt
git commit -m "Give AnimatedExpand spring physics instead of linear tweens"
```

---

### Task 7: Motion — shared-axis nav transition

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/TimeGoNavHost.kt`

- [ ] **Step 1: Replace the imports**

Change:
```kotlin
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
```
to:
```kotlin
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
```

- [ ] **Step 2: Replace each `composable(...)` block's transitions**

Change all three occurrences of:
```kotlin
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(150)) },
```
to:
```kotlin
                enterTransition = { slideInHorizontally(tween(220)) { it / 8 } + fadeIn(tween(220)) },
                exitTransition = { slideOutHorizontally(tween(180)) { -it / 8 } + fadeOut(tween(180)) },
```

This is Material's shared-axis-X pattern (per `android.md`'s "Material motion patterns" guidance) — a small horizontal displacement (1/8 of screen width) paired with the fade, so switching tabs reads as one page sliding to replace another rather than a plain crossfade. Kept as a tween (not spring) since a full-bleed screen swap wants a predictable, bounded duration — the spring treatment is reserved for local expand/collapse (Task 6) and content swaps (Task 8), where a little overshoot reads as tactile rather than distracting.

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/TimeGoNavHost.kt
git commit -m "Replace bottom-nav crossfade with a shared-axis slide transition"
```

---

### Task 8: Motion — spring-based Progress screen content swaps

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt` (imports + two `AnimatedContent` blocks only)

- [ ] **Step 1: Replace the animation imports**

Change:
```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
```
to:
```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
```

- [ ] **Step 2: Replace both `transitionSpec` blocks**

There are two identical occurrences (PR tile swap on exercise change, strength-curve swap on wheel-picker change). Change each:
```kotlin
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200))) togetherWith
                                    (fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)))
                            },
```
to:
```kotlin
                            transitionSpec = {
                                (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))) togetherWith
                                    (fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessHigh)))
                            },
```
(indentation will differ slightly between the two call sites in the actual file — match each block's existing indentation, only the animation spec content changes)

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Give Progress screen content swaps spring physics"
```

---

### Task 9: Re-pick the muscle heatmap scale within the Ledger palette

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleHeatColor.kt` (`HEAT_STOPS` values only)
- Modify: `app/src/test/java/com/lsing/timego/domain/MuscleHeatColorTest.kt` (two literal assertions only)
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt` (`HeatmapGrid` color params)

**Interfaces:** `heatColor`/`heatStopHexes`/`recolorByLightness` signatures unchanged — this is a color-constant change only, the interpolation/lightness math is untouched (per the spec: restyle, not redesign).

- [ ] **Step 1: Replace `HEAT_STOPS` in `MuscleHeatColor.kt`**

Change:
```kotlin
private val HEAT_STOPS = listOf(
    "#22D3EE", // low
    "#3B82F6",
    "#8B5CF6",
    "#EC4899",
    "#EF4444", // high
)
```
to:
```kotlin
private val HEAT_STOPS = listOf(
    "#8A8367", // low -- untrained/cool reads as flat graphite-tan, not a "cold" hue
    "#C98A2E", // amber -- matches LedgerPlateauing
    "#D9622E",
    "#C23B25",
    "#8C2C22", // high -- matches LedgerAccentDim
)
```
Update the doc comment above it (currently "Cyan-to-red intensity scale... cyan, blue, violet, pink, red") to: `/** Graphite-to-ember intensity scale for the muscle heatmap (low recent volume -> high), re-picked within the Training Ledger palette -- graphite/tan through amber to the accent's brick-red, evenly-spaced stops interpolated as before. */`

- [ ] **Step 2: Update the two literal test assertions in `MuscleHeatColorTest.kt`**

Change:
```kotlin
        assertEquals("#22D3EE", heatColor(0f))
        assertEquals("#EF4444", heatColor(1f))
```
to:
```kotlin
        assertEquals("#8A8367", heatColor(0f))
        assertEquals("#8C2C22", heatColor(1f))
```
Update the test name from `` `heatColor at the extremes matches the legend's cyan and red stops` `` to `` `heatColor at the extremes matches the legend's graphite and ember stops` ``. Leave every other test in the file untouched — they assert relative/structural properties (monotonicity, hex format, clamping) that hold for any monotonic warm-toned scale.

- [ ] **Step 3: Update `HeatmapGrid` colors in `ProgressScreen.kt`**

Change (`ProgressScreen.kt`, inside the `Consistency` section item):
```kotlin
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = Color(0xFF7FD8A0),
                darkColor = Color(0xFF1B5E3A),
                onDateClick = { date -> viewModel.selectHistoryDate(date) },
            )
```
to:
```kotlin
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = Color(0xFFD9622E),
                darkColor = Color(0xFF5C1A14),
                onDateClick = { date -> viewModel.selectHistoryDate(date) },
            )
```
(Consistency heatmap uses its own independent green scale today — moved onto the same ember family as the muscle heatmap so the two heat visualizations read as one system, not two unrelated color languages.)

- [ ] **Step 4: Build + test verify**

Run: `./gradlew :app:compileDebugKotlin && ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass including the two updated `MuscleHeatColorTest` assertions.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleHeatColor.kt app/src/test/java/com/lsing/timego/domain/MuscleHeatColorTest.kt app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Re-pick heatmap color scales within the Ledger palette"
```

---

### Task 10: Apply to LogScreen — ruled rows, mono numerals

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt` (full rewrite)

**Interfaces:** unchanged public surface (`LogScreen(viewModel: LogViewModel)`); `ExerciseRowHeader`/`ExerciseCard`/`StrengthLogRow`/`CardioLogRow`/`HoldLogRow` stay private, no downstream callers.

- [ ] **Step 1: Replace the full contents of `LogScreen.kt`**

Apply these changes to the current file (read `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt` fresh before editing — Task 6's `AnimatedExpand` signature is unchanged so those call sites don't need touching):

1. **Add import**: `import androidx.compose.foundation.HorizontalDivider` and `import com.lsing.timego.ui.theme.LedgerFigureValue` (for numeric text) — remove nothing else from the import list.
2. **`ExerciseCard`**: replace the elevated-`Card` + 3dp-accent-bar structure with a ruled hairline divider below each row and the accent bar shown *only* when `expanded` is true (needs threading an `expanded: Boolean` param through, or reading it from the caller — simplest: move the accent-bar `Box` inside each of `StrengthLogRow`/`CardioLogRow`/`HoldLogRow` where `expanded` is already in scope, conditionally rendered):

```kotlin
/** Replaces the elevated-card-with-permanent-accent-bar treatment: a plain surface with a
 *  hairline bottom rule (ledger row divider) and the category accent shown only on the active
 *  row, not on every row permanently -- restraint is the point of the direction. */
@Composable
private fun ExerciseCard(accent: Color, expanded: Boolean, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.Large, end = Spacing.Small)
            .then(
                if (expanded) Modifier.background(accent.copy(alpha = 0.06f)) else Modifier,
            ),
    ) {
        content()
        HorizontalDivider(
            color = if (expanded) accent else MaterialTheme.colorScheme.outlineVariant,
            thickness = if (expanded) 2.dp else 1.dp,
        )
    }
}
```
(Drop the `IntrinsicSize`/`fillMaxHeight`/`width(3.dp)` left-bar imports and usage entirely — no longer needed with the divider-based approach. Remove the now-unused `import androidx.compose.foundation.layout.Box`, `import androidx.compose.foundation.layout.IntrinsicSize`, `import androidx.compose.foundation.layout.fillMaxHeight`, `import androidx.compose.foundation.layout.width` if nothing else in the file uses them — check before removing, `Box`/`width` may still be used elsewhere in the file.)

3. **Every `ExerciseCard(visual.accent) { ... }` call site** (in `StrengthLogRow`, `CardioLogRow`, `HoldLogRow`) becomes `ExerciseCard(visual.accent, expanded) { ... }`.
4. **Numeric text**: every `OutlinedTextField` for kg/reps/seconds/minutes/km, and every suggestion `Text` showing a logged/suggested number, gets `style = LedgerFigureValue` alongside its existing modifier (add the param; `OutlinedTextField`'s `textStyle` param, not the field's `label`, carries this — labels stay on Manrope/`bodySmall` since they're words, not numbers). Suggestion `Text` composables (currently `style = MaterialTheme.typography.bodySmall`) split their string: the number portion uses `LedgerFigureValue`, the surrounding words stay `bodySmall` -- simplest correct approach is an `AnnotatedString` with two spans, but a pragmatic first pass (whole suggestion line in `LedgerFigureValue` at a smaller size) is acceptable if `AnnotatedString` proves fiddly; note the simplification in the commit message if taken.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Common failure: leftover unused imports (`IntrinsicSize`, etc.) — Kotlin only warns on these, doesn't fail the build, but clean them up anyway.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Apply ruled-row ledger treatment and mono numerals to LogScreen"
```

---

### Task 11: Retheme `HorizontalWheelPicker`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/HorizontalWheelPicker.kt`

**Context:** most of this component already re-themes automatically once Task 3 lands (it reads `MaterialTheme.colorScheme.primary`/`onSurfaceVariant` and `MaterialTheme.typography.titleMedium`/`bodyMedium`, all of which now resolve to Ledger tokens). The one concrete ledger-specific addition is a hairline center-tick mark, so the picker reads as a ruled instrument rather than a bare label list.

- [ ] **Step 1: Add a centered tick mark**

Add import: `import androidx.compose.foundation.layout.Column` and `import androidx.compose.material3.MaterialTheme` (if not already present) and `import androidx.compose.foundation.Canvas` and `import androidx.compose.ui.graphics.drawscope.Stroke`.

Wrap the existing `BoxWithConstraints` content in a `Column`, adding a short vertical tick centered below the wheel:

```kotlin
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // ... existing sidePadding/LazyRow content unchanged, `modifier` param no longer applied here (moved to the outer Column) ...
        }
        Canvas(modifier = Modifier.width(2.dp).height(6.dp)) {
            drawLine(
                color = LedgerAccentColor, // resolves via MaterialTheme.colorScheme.primary at call site, see note below
                start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                strokeWidth = size.width,
            )
        }
    }
```

Replace the placeholder `LedgerAccentColor` with `MaterialTheme.colorScheme.primary` captured as a local val before the `Canvas` block (Canvas's `DrawScope` isn't `@Composable`, so read the color outside it: `val tickColor = MaterialTheme.colorScheme.primary` then reference `tickColor` inside `drawLine`).

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/HorizontalWheelPicker.kt
git commit -m "Add ledger tick mark to HorizontalWheelPicker"
```

---

### Task 12: Apply to ProgressScreen — mono stat values, ledger cards

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

- [ ] **Step 1: Swap the Fraunces import**

Change:
```kotlin
import com.lsing.timego.ui.theme.FrauncesStatValue
```
to:
```kotlin
import com.lsing.timego.ui.theme.LedgerFigureValue
```

- [ ] **Step 2: Update `StatTile`**

Change:
```kotlin
            Text(value, style = FrauncesStatValue)
```
to:
```kotlin
            Text(value, style = LedgerFigureValue)
```

- [ ] **Step 3: Mono treatment for other logged numbers**

Apply `style = LedgerFigureValue` (or a smaller-size variant, `LedgerFigureValue.copy(fontSize = 14.sp)`, where `bodyMedium`/`bodySmall` was the prior size) to: the BMI line (`"BMI: %.1f (...)"`), the `${metric.date}: ${metric.weightKg}...` body-metrics list rows, and the `DayHistoryDialog`'s per-set numbers. Labels/captions (exercise names, "Workouts"/"Duration" labels, dates formatted as text) stay on Manrope.

- [ ] **Step 4: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Apply ledger mono numerals across ProgressScreen"
```

---

### Task 13: Apply to RoutinesScreen — ledger routine names, ruled rows

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt` (full rewrite)

- [ ] **Step 1: Replace the full contents of `RoutinesScreen.kt`**

```kotlin
package com.lsing.timego.ui.routines

import androidx.compose.foundation.HorizontalDivider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()
    var showRoutineForm by remember { mutableStateOf(false) }

    if (showRoutineForm) {
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { showRoutineForm = false },
            onCreate = viewModel::createRoutine,
        )
    }

    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
        if (untrainedGroups.isNotEmpty()) {
            item {
                Text(
                    "Not trained in a while: ${untrainedGroups.joinToString(", ") { formatEnumLabel(it) }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item {
            SectionHeader(
                title = "Your Routines",
                topPadding = Spacing.ExtraSmall,
                trailing = { Button(onClick = { showRoutineForm = true }) { Text("+ New routine") } },
            )
        }
        if (routines.isEmpty()) {
            item {
                Text(
                    "No routines yet -- create one to plan which days you train which exercises.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(routines, key = { it.id }) { routine ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        routine.name,
                        style = LedgerFigureEmphasis,
                        modifier = Modifier.weight(1f).padding(Spacing.Medium, Spacing.Medium, Spacing.Medium, Spacing.ExtraSmall),
                    )
                    IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${routine.name}")
                    }
                }
                if (routine.daysOfWeek.isEmpty()) {
                    Text(
                        "No days set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.Medium, 0.dp, Spacing.Medium, Spacing.Medium),
                    )
                } else {
                    FlowRow(modifier = Modifier.padding(Spacing.Small, 0.dp, Spacing.Small, Spacing.Small)) {
                        routine.daysOfWeek.forEach { day ->
                            AssistChip(
                                onClick = {},
                                label = { Text(day.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                                modifier = Modifier.padding(Spacing.ExtraSmall),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
```

Note: this replaces both `Card`-wrapped sections (untrained-groups banner, per-routine entry) with plain `Column`s separated by `HorizontalDivider` rules — the same ruled-row treatment as `LogScreen` (Task 10), rather than each screen inventing its own version of "ledger row." `CardDefaults` import is dropped since no `Card` remains in this file.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt
git commit -m "Apply ruled-row ledger treatment and mono routine names to RoutinesScreen"
```

---

### Task 14: Sweep for any remaining `Onyx`/`Fraunces` references

**Files:** none pre-determined — this task is a grep sweep.

- [ ] **Step 1: Grep the whole module**

```bash
grep -rn "Onyx\|Fraunces" app/src/main/java app/src/test 2>&1
```
Expected: no results. If any remain (e.g. a screen not touched by Tasks 10–13 that still references `OnyxPrimary` directly for a one-off tint, or a test asserting on a Fraunces-specific value), fix them individually — grep gives the exact file/line.

- [ ] **Step 2: Commit if anything was fixed**

```bash
git add -A
git commit -m "Sweep remaining Onyx/Fraunces references"
```
(Skip this commit if Step 1 found nothing.)

---

### Task 15: Full build + test verification + vault note update

**Files:** none (verification only) + vault note

- [ ] **Step 1: Full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests green including the two updated `MuscleHeatColorTest` assertions from Task 9.

- [ ] **Step 3: `./gradlew installDebug` and hand off**

Per established project preference, do not screenshot proactively — install and let the user verify on their Galaxy S23 Ultra: dark mode (primary), light mode, all three screens, an exercise row expand/collapse, a PR/plateau state if one is reachable in current data, and the bottom-nav transition between tabs.

- [ ] **Step 4: Update the vault project note**

Add a session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` (top of file, under the existing `[!note]` callout) recording: Training Ledger redesign shipped (color/type/shape/motion foundation + all three screens), branch `training-ledger-redesign`, spec+plan filed under `docs/superpowers/specs`/`docs/superpowers/plans`, on-device verification pending until the user confirms. Note the direction-selection process (Impeccable's seed script, assigned "Street Rig Signage" vs. chosen "Training Ledger" pick) as context for why this particular world was chosen, matching how the muscle-group/PR/visual-identity sessions are documented.

- [ ] **Step 5: Merge once approved**

After the user confirms on-device, merge `training-ledger-redesign` to `master` (fast-forward if possible, matching every prior TimeGo branch) and delete the branch.

---

## Self-Review Notes

- **Spec coverage**: Section 1 (color) → Task 2–3. Section 2 (type) → Task 1, 4. Section 3 (shape/structure) → Task 5, 10, 13. Section 4 (motion) → Task 6–8. Section 5 (Log) → Task 10. Section 6 (Progress) → Task 9 (heatmap), 11 (wheel picker), 12 (stat values/mono). Section 7 (Routines) → Task 13. Application order & verification → Tasks 1–15 follow the spec's foundation-first, then Log → Progress → Routines order exactly.
- **Deliberate build-red checkpoint**: Task 2 commits with a known-broken build (Theme.kt not yet updated) to keep one logical change per commit, matching this plan's per-task granularity; Task 3 fixes it immediately after. Flagged explicitly in Task 2 so whoever executes this doesn't mistake it for a mistake.
- **Domain-layer touch is scoped and justified**: Task 9 is the only task touching `domain/` (`MuscleHeatColor.kt`) — a color-constant change, not a logic change, with its test updated in the same task to keep the literal assertions truthful. No other domain/ViewModel file is touched anywhere in this plan.
- **No dangling references**: Task 14 exists specifically to catch anything Tasks 2–13 missed (a stray `Onyx*`/`Fraunces*` reference in a file this plan didn't anticipate) before the final verification pass.
- **Out of scope respected**: no task touches `RadarChart.kt`, `SparklineChart.kt`, `MuscleBodyDiagram.kt`, or `MuscleBodyArt.kt`'s actual chart/diagram math — only the color constants those visuals draw from (`MuscleHeatColor.kt`, Task 9) and their surrounding chrome (theme cascade). `HorizontalWheelPicker`'s scope (Task 11) is a small addition, not the rollout-to-new-fields the spec explicitly excludes.
