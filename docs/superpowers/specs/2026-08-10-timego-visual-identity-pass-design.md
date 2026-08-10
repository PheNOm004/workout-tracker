# TimeGo — Full Visual-Identity Pass (design)

**Date**: 2026-08-10
**Status**: approved, not yet implemented
**Context**: third of three sequenced frontend sessions ("go through the frontend now for timego", requested 2026-08-09). Muscle-group tagging and Personal Records redesign are both done and merged to `master`. This is the last item: a broad typography/color/spacing/consistency pass across the whole app, deferred from Update 1.1 for lack of budget at the time (see `2026-08-09-timego-update-1.1-design.md`).

**Trigger**: user reports the app "feels flat/generic" day to day, despite already having a distinctive underlying theme (the "Onyx" dark palette in `ui/theme/Color.kt`, Manrope + Fraunces type scale in `ui/theme/Type.kt`, custom shape scale in `ui/theme/Shapes.kt`, all reused from HeatP). The tokens exist but aren't actually differentiating anything on screen — every screen currently uses bare `Card`, default `Button`, plain `Text` with no icons, no accent color, and Fraunces (the "moment" serif) isn't called anywhere in the app despite being defined.

**No phone connected for this session** — code the full pass, build-verify only (`assembleDebug` + existing unit tests), defer on-device verification to when the user reconnects their Galaxy S23 Ultra.

## Approach

Shared visual-language kit, built once in `ui/common`, then rolled out screen by screen (Log → Routines → Progress touch-ups). Chosen over a bespoke per-screen redesign (risks the three screens feeling like different apps) or minimal spacing-only polish (the actual complaint is that components themselves are generic, not just their spacing) — and it matches the project's established pattern of small reusable pieces (`StatTile`, `HorizontalWheelPicker`) built once and reused.

## Section 1 — Category visual language

New `CategoryVisuals.kt` in `ui/common`:

```kotlin
data class CategoryVisual(val icon: ImageVector, val accent: Color)
fun categoryVisual(category: ExerciseCategory): CategoryVisual
```

Mapping:

| Category | Icon | Accent |
|---|---|---|
| STRENGTH | `Icons.Filled.FitnessCenter` | `OnyxPrimary` |
| CALISTHENICS | `Icons.Filled.Accessibility` | `OnyxTertiary` |
| CARDIO | `Icons.Filled.MonitorHeart` (or `DirectionsRun` if unavailable in the Material icon set actually bundled) | `OnyxSecondary` |
| WARMUP | `Icons.Filled.Whatshot` | an outline-family neutral (deliberately muted — warmups aren't the main event) |

Applied as: a 2dp left border on each exercise card using the category's accent (not a full-card tint, to preserve the Onyx palette's restraint), plus the icon shown in `ExerciseRowHeader` next to the exercise name, tinted with the same accent.

## Section 2 — Card & spacing conventions

- **Elevation**: flat `Card()` → `Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp))` everywhere. Onyx's dark surfaces currently have no visual separation from the background at rest.
- **Spacing scale**: new `Spacing.kt` with named constants (4/8/12/16/24 dp) so screens stop guessing at ad-hoc padding values. Standardize: card internal padding 12dp, inter-card gaps 8dp, screen-edge padding 16dp (already the de facto value on most screens — just made consistent everywhere).
- **Section headers**: new reusable `SectionHeader` composable (label + optional trailing action) replacing the current plain `titleMedium` `Text` calls used inconsistently for "Session type" (Log) and "Your Routines" (Routines).

## Section 3 — Typography: giving Fraunces real jobs

Fraunces is currently only referenced by `display*`/`headlineLarge` styles that no screen actually calls — it's dead weight in the type system today. This pass gives it two real jobs, leaving everything else (body text, inputs, buttons, chips, exercise names) on Manrope:

- **PR stat values** in `ProgressScreen`'s `StatTile`s — the number itself (e.g. "82.5kg") renders Fraunces-SemiBold (`headlineMedium`-family), label stays Manrope.
- **Routine names** in `RoutinesScreen` cards — swap from plain `titleMedium` to a Fraunces-based style; routine names are short and identity-bearing ("Push Day").

## Section 4 — Application order & verification

**Order**: LogScreen (category-cue work lands here) → RoutinesScreen (routine-name typography, section header, card elevation) → Progress touch-ups (StatTile Fraunces, section-header consistency — Progress already got real design attention across the last two sessions, so this is bringing it in line with the new card/spacing conventions, not a redesign).

**Verification**: no new domain logic in this pass (pure UI), so no new unit tests needed. Verify via `./gradlew assembleDebug` plus the existing unit test suite staying green. No proactive screenshots (per established project preference — the user verifies on-device themselves). On-device walkthrough happens when the phone reconnects; follow-up fixes come from that real feedback rather than speculative pre-emptive changes.

## Out of scope

- Progress screen's chart/diagram *logic* — only card/spacing/typography convention alignment, not new features.
- Broader `HorizontalWheelPicker` rollout beyond its current two selectors (user said "other not now" previously — not raised again this session).
- Shoulder/abs muscle-diagram zone-classification refinement (unrelated, separate deferred item).
