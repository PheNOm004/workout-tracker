# TimeGo v1 — Gym Progress & Workout Tracker

Brainstormed 2026-08-09. Spin-off concept from HeatP (`C:\Users\lsing\AndroidStudioProjects\HeatP`), stripped to gym-specific tracking. Vault note: `TimeGo - Gym Progress Tracker.md`.

## Purpose

A personal, fully-local Android app for logging gym workouts, tracking progress over time, and getting simple on-device recommendations for what to lift next. No meal tracking, no ML, no network dependency in v1.

## Scope (v1)

1. **Workout logging** — both routine-based (pre-built templates) and freeform (add exercises on the fly) sessions; either mode allows adding/removing exercises mid-session. Each exercise shows its last-logged weight/reps plus the current suggestion inline while logging sets.
2. **Progress tracking**:
   - Per-exercise strength curve (weight / est. 1RM over time)
   - Auto-detected PR badges (heaviest weight, most reps, best volume)
   - Consistency heatmap (workout days + volume), reusing HeatP's `HeatmapGrid` composable unchanged
   - Body metrics (weight, measurements), manually logged, own trend chart
3. **Recommendations** — two independent, deterministic, on-device modules behind a shared interface so a smarter implementation can be swapped in later without touching UI code:
   - **Overload suggester**: next-session target per exercise from last performance + trend, with a deload rule if reps were missed twice in a row
   - **Muscle-balance nudge**: flags muscle groups untrained for N days, using per-exercise muscle-group tags
4. **Exercise library** — pre-loaded common exercises (pre-tagged by muscle group) plus user-added custom exercises (also taggable)

## Explicitly Deferred

- Meal tracking / meal recommendations
- ML-based prediction (revisit once real multi-month training history exists)
- Social/sharing features
- Wearable/Health Connect integration

## Tech Stack

Native Android, Kotlin, Jetpack Compose, Room — same as HeatP. Package `com.lsing.timego`, project at `C:\Users\lsing\AndroidStudioProjects\TimeGo`. Min SDK 26, compileSdk 37.1, same Gradle/AGP/Kotlin/KSP versions as HeatP (Gradle 9.5.0, AGP 9.3.0, Kotlin 2.2.10, Compose BOM 2026.02.01, Room 2.8.4).

## What Was Scaffolded (done, this session)

The project shell was copied from HeatP and pruned rather than authored from scratch, to conserve tokens on boilerplate that carries no gym-specific logic. See commit `9ae540a` on `master` for the exact diff.

**Reused verbatim** (self-contained, no HeatP domain-type coupling — confirmed by inspection before copying):
- `ui/common/HeatmapGrid.kt` — the `HeatmapGrid` composable (`Map<LocalDate, Float>` → rendered heatmap) and its private `HeatmapWeekDots` helper, lifted from HeatP's `SummaryScreen.kt`. Will back the v1 consistency heatmap directly — takes ratios in, renders "last 18 weeks" or full scrollable year, no ViewModel dependency.
- `domain/HeatmapColorMath.kt` — `habitHeatmapColorHexes`-style HSV-derived light/dark color pair for a given hex color. Pure Kotlin, no `android.graphics` dependency, unit-tested.
- `domain/YearWindow.kt` — calendar month/year window math, pure Kotlin.
- `ui/theme/{Color,Shapes,Theme,Type}.kt` — Material3 theme tokens and the bundled Fraunces/Manrope variable fonts. Kept HeatP's "Onyx" cool dark-mode identity as TimeGo's starting point pending a rename/rebrand decision (not blocking v1 functionality).
- Room `LocalDate <-> epochDay` `TypeConverter` pattern, and the ASCII-unit-separator (`\u001F`) string-list converter pattern, both in `data/Converters.kt` — kept, HeatP's enum-specific converters (`TrackingType`, `ReminderMode`, `MetricType`) dropped since those types don't exist in TimeGo.
- Gradle/Kotlin/KSP toolchain and workarounds: `android.disallowKotlinSourceSets=false` (KSP/AGP9 bug workaround, still needed), `gradle/gradle-daemon-jvm.properties` (toolchain 21 auto-provisioning).

**Not ported** — pattern-only in HeatP (imports `Habit`/`HabitLog`/`TrackingType`, would not compile against TimeGo's domain) or out of v1 scope entirely:
- `Habit`, `HabitDao`, `HabitLog`, `HabitRepository`, `HeatPDatabase`, `StreakCalculator`, `SummaryMath`, `HeatmapMath.kt`, all `ui/habit/*` screens, `ui/summary/SummaryViewModel.kt`, `HeatPNavHost.kt` — habit-specific; TimeGo's own `Exercise`/`Routine`/`SetLog` schema and screens are built fresh in the implementation plan.
- `mail/`, `schedule/`, `health/`, `reminder/` packages and their Gradle dependencies (`msal`, `biweekly`, `androidx-work-runtime`, `androidx-health-connect-client`) and the Duo-SDK-Feed Maven repo — entirely out of scope (no email schedule sync, no Health Connect, no background reminders in v1).
- `AndroidManifest.xml` stripped to a bare single-activity app (no calendar/health/boot/MSAL-redirect permissions or components).

**Verified**: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` both pass on the pruned scaffold (`HeatmapColorMathTest`, `YearWindowTest`, and the default instrumented/unit test templates, all repackaged to `com.lsing.timego`).

## Data Model (sketch — full schema is an implementation-plan concern)

- `Exercise` — name, muscle group(s) (`List<String>` via the kept converter), custom flag
- `Routine` / `RoutineExercise` — optional templates
- `WorkoutSession` — date (`LocalDate` via the kept converter), optional routine link
- `SetLog` — session, exercise, weight, reps, timestamp (the core logging unit)
- `BodyMetric` — date, weight, optional measurements

## Logging Flow

Start a session either from a saved routine (pre-fills expected exercises) or freeform (add from library on the fly); either way, exercises can be added/removed mid-session. Each exercise row shows last-logged weight/reps and the current overload suggestion while logging sets.

## Recommendation Engine

Two modules behind a shared interface (name TBD in the implementation plan, e.g. `RecommendationEngine`), both deterministic and on-device:

- **Progressive overload suggester**: given an exercise's logged history, suggest next-session weight/reps from last performance + trend; apply a deload rule if the last two sessions missed the target rep count.
- **Muscle-balance nudge**: given recent sessions' exercises and their muscle-group tags, flag any muscle group untrained for more than N days (N configurable, default TBD in plan).

Designed behind an interface specifically so a data-driven/ML approach can replace either module later without UI changes — not because ML is planned for v1 (single-user lifting data is too sparse per exercise at launch to train anything meaningfully beyond these heuristics).

## Naming

Tentative app/package name: **TimeGo** (`com.lsing.timego`). Not finalized — revisit if a better name comes up before v1 ships.

## Out of Scope for This Spec

Detailed Room schema (columns, migrations), screen-by-screen UI spec, exact overload/deload thresholds, and muscle-group tag taxonomy are implementation-plan concerns, not design-spec concerns — kept at sketch level here per YAGNI.
