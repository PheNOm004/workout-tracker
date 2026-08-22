# TimeGo Backlit Batch A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the Backlit depth foundation so existing Material 3 surfaces gain readable dark-mode separation without changing screens or product behaviour.

**Architecture:** Keep the existing Night Training Console palette and Material 3 theme as the sole source of colour roles. Add a small set of Backlit tokens, remap only dark container roles, and provide a reusable `SurfaceCard` composable for later batches; Batch A does not migrate any call sites.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Gradle Android application plugin.

**Spec:** `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo\14 Backlit UI Audit and Implementation Record.md`

## Global Constraints

- Preserve the Night Training Console identity; Backlit deepens it and does not introduce a fourth visual identity.
- Do not change application behaviour, Room schema, adaptive-coach code, permissions, networking, or dependencies.
- Dark-mode depth uses tonal steps, 1dp hairlines, and restrained light; no new drop shadows.
- Glow is reserved for navigation, primary FAB, chart series, and later pulse moments; Batch A introduces the token only.
- Keep existing `Ledger*` names and all light-theme mappings intact.
- Do not stage, amend, or otherwise alter the already-staged secret-remediation files (`.gitignore` and `ml-prototype/jupyter.log`).
- Run the complete JVM unit suite and debug build. Install only after those pass; the user performs on-device contrast verification before any Batch B–D work or a Batch A commit.

---

### Task 1: Add Backlit theme contract, tokens, and typography roles

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Type.kt`

**Interfaces:**
- Produces colour tokens `NightDeckLow`, `NightDeckHigh`, `NightEdgeHairline`, `NightSheenTop`, and `NightGlow` for later Backlit components.
- Produces typography values `NightEyebrow` and `LedgerFigureHero` for later screen migrations.
- Changes only `DarkColorScheme.surfaceContainer`, `surfaceContainerHigh`, and `surfaceContainerHighest`; existing cards inherit the new deck contrast through Material 3.

- [x] **Step 1: Record the pre-change compilation baseline**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`; this confirms any later compiler failure is attributable to Batch A.

- [x] **Step 2: Add the five Backlit colours to `Color.kt`**

Append these immutable `Color` values after the existing Night palette aliases:

```kotlin
val NightDeckLow = Color(0xFF1A2125)
val NightDeckHigh = Color(0xFF232C30)
val NightEdgeHairline = Color(0x12FFFFFF)
val NightSheenTop = Color(0x08FFFFFF)
val NightGlow = Color(0x2AFF6B5E)
```

- [x] **Step 3: Replace the obsolete Training Ledger direction comment and remap the three dark container roles**

Replace the opening Theme contract with the seven Backlit rules from the spec. In `DarkColorScheme`, make precisely these substitutions:

```kotlin
surfaceContainer = NightDeckLow,
surfaceContainerHigh = NightDeckHigh,
surfaceContainerHighest = Color(0xFF2E373B),
```

Leave `surfaceContainerLow`, every other dark role, and the complete `LightColorScheme` unchanged.

- [x] **Step 4: Add the two Backlit typography values to `Type.kt`**

Append these styles after `LedgerFigureValue`/`LedgerFigureEmphasis`:

```kotlin
val NightEyebrow = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp,
)

val LedgerFigureHero = TextStyle(
    fontFamily = LedgerMonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
)
```

- [x] **Step 5: Compile the changed theme boundary**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`; no undefined token or Material 3 colour-role error.

### Task 2: Add the reusable Backlit surface primitive

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/SurfaceCard.kt`

**Interfaces:**
- Consumes: `NightDeckLow`, `NightDeckHigh`, `NightEdgeHairline`, `NightSheenTop`, and `NightGlow` from Task 1.
- Produces: `@Composable fun SurfaceCard(modifier: Modifier = Modifier, hero: Boolean = false, glow: Boolean = false, content: @Composable BoxScope.() -> Unit)`.
- Later batches may use `hero = true` for a top sheen and `glow = true` only within the documented glow budget; default cards are a deck plus hairline.

- [x] **Step 1: Create `SurfaceCard.kt` with the stable public interface**

Declare the composable exactly as follows:

```kotlin
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    glow: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
)
```

- [x] **Step 2: Implement a clipped 16dp deck and 1dp edge**

Use `RoundedCornerShape(16.dp)`, select `NightDeckHigh` when `hero` is true and `NightDeckLow` otherwise, then apply `.clip(shape)`, `.background(deck)`, and `.border(1.dp, NightEdgeHairline, shape)` to the backing `Box`.

- [x] **Step 3: Add only the documented optional light effects**

Within `drawBehind`, draw a vertical gradient from `NightSheenTop` to transparent over the upper 35% when `hero` is true. When `glow` is true, draw a coral `Brush.radialGradient` centred at the lower horizontal midpoint with `NightGlow` fading to transparent and a radius of `size.maxDimension * 0.85f`. Do not use shadow modifiers.

- [x] **Step 4: Compile the new shared component**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`; the new primitive has no call sites in Batch A, so compilation is the boundary test.

### Task 3: Verify the foundation without changing product behaviour

**Files:**
- Verify only: the Task 1 and Task 2 files

**Interfaces:**
- Consumes: the compiled Backlit theme and `SurfaceCard` primitive.
- Produces: a debug APK ready for the user’s AMOLED contrast review; no database, API, or feature contract changes.

- [x] **Step 1: Run all JVM unit tests**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` with the existing unit suite green.

- [x] **Step 2: Build the debug APK**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat assembleDebug`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [x] **Step 3: Install the debug build after build verification**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat installDebug`

Expected: `BUILD SUCCESSFUL`; the device keeps existing data because this is an in-place debug install.

- [x] **Step 4: Request the user’s on-device verification**

Ask the user to open Log, Progress, Routines, a dialog, and a bottom sheet in dark mode. Acceptance: standard card decks are visibly distinct from the `#101315` ground without looking grey; elevated sheets remain above cards; there are no new shadows, colour regressions, or readability losses.

- [x] **Step 5: Commit only after the user accepts the device result**

Stage only the three theme files, `SurfaceCard.kt`, and this plan after reviewing each diff. Do not include the pre-existing staged secret-remediation files. Commit message:

```text
feat(ui): add Backlit depth foundation
```

### Task 4: Apply the foundation to representative visible surfaces

**Reason for revision:** Device verification showed that token remapping alone was visually negligible because the primary Log cards use `surfaceContainerLow`, the Progress card had only a tiny tonal shift, and Routines had no card surface. The user approved this narrow visible-foundation pass on 2026-08-22.

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/SurfaceCard.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/common/WorkoutHistoryDialog.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt`

**Interfaces:**
- `SurfaceCard` remains the sole shared Backlit primitive and must preserve an incoming clickable modifier.
- The Log last-session card, recommended card, Progress PR deck/stat tiles, and routine rows receive a visible tonal deck and hairline edge.
- No callbacks, state, data, copy, schema, or adaptive-coach behaviour change.

- [x] **Step 1: Preserve input interaction when drawing a surface**

Apply the card's clip before the supplied modifier so a caller's `clickable` ripple remains inside the rounded deck.

- [x] **Step 2: Replace the two Log landing `Surface` containers with `SurfaceCard`**

Keep the existing last-session click callback and all existing child content; make the last-session card a hero deck and the recommendation a standard deck.

- [x] **Step 3: Replace the Progress PR deck and shared stat tiles with `SurfaceCard`**

Make the PR deck a hero. Use standard decks for `StatTile`, preserving its modifier, padding, labels, values, and captions for both Progress and workout-history consumers.

- [x] **Step 4: Wrap each routine row in a standard `SurfaceCard`**

Keep the existing delete action, day chips, divider, ordering, and spacing. The new card only supplies the deck and edge.

- [x] **Step 5: Compile, run JVM tests, rebuild, and reinstall**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat :app:compileDebugKotlin; if ($?) { .\gradlew.bat testDebugUnitTest }; if ($?) { .\gradlew.bat installDebug }`

Expected: all commands succeed and the visible deck treatment appears on Log, Progress, and Routines.

## Self-Review

- **Spec coverage:** Task 1 implements A1–A3; Task 2 implements A4; Task 3 implements the specified build, install, user-review, and commit discipline. No later Batch B–E visual or behaviour changes are included.
- **Scope:** Task 4 intentionally migrates only representative surface call sites; it does not alter screen behaviour or adaptive-coach state.
- **Consistency:** `SurfaceCard` consumes only Task 1 tokens and exposes the exact `hero`/`glow` interface named by the specification.
