# TimeGo — Training Ledger Redesign (design)

**Date**: 2026-08-11
**Status**: draft, pending user approval
**Context**: the Aug 10 visual-identity pass (icons, 2dp elevation, `SectionHeader`, Fraunces on two spots) is done and device-verified, but the user still calls the app crude — this time app-wide, not one screen — and separately calls the existing motion system (`AnimatedExpand`, nav crossfade, `AnimatedContent` swaps) generic/flat. This is a full redesign, not another polish pass on the same identity: the Onyx dark palette, Manrope/Fraunces pairing, and current motion grammar are all explicitly in scope to be replaced, confirmed by the user (`Everything is in play`) after being asked directly whether Onyx specifically was still a fixed constraint.

**Trigger**: `/impeccable` redesign flow, run against a fresh `PRODUCT.md` (repo root) since none existed. Direction chosen through Impeccable's mandatory direction-seed script (`concept-seed.mjs --scope direction --mode operate`), which returned an assigned grounded candidate (Street Rig Signage) and let the user pick a different grounded candidate from the same list instead (Training Ledger, presented as `MY PICK`). Full direction contract recorded as a comment at the top of `ui/theme/Theme.kt`.

## Direction

**Training Ledger** — a serious lifter's paper training logbook, rendered dark-first rather than the cream/parchment default: graphite graph-paper grid on a near-black page (dark, primary) or bone paper (light, still first-class, not an afterthought invert). Tabular monospace numerals carry every logged value. One committed red margin-rule accent (brick/ink red, not neon) carries state and primary action. Ruled horizontal lines replace card-shaped chrome as the main structural device — this is a page, not a dashboard of boxes.

Screens read as ledger pages: Log is today's page (suggested targets already "penciled in"), Progress is flipping back through past pages (curves/PRs/heatmap), Routines is a bookmarked template page.

## Section 1 — Color tokens

New `Ledger*` token family in `ui/theme/Color.kt`, replacing every `Onyx*` role (all consumed explicitly in `Theme.kt`'s `darkColorScheme`/`lightColorScheme`, so every current role gets a direct replacement, not a partial swap):

- **Dark (primary mode)**: near-black warm-neutral graphite ground (not the current cool blue-black `0xFF0A0A0C` — warmer, closer to a page under a desk lamp), graphite-gray grid/rule lines for `outline`/`outlineVariant`, bone-white ink for `onSurface`.
- **Light**: real bone/aged-paper ground, graphite ink for `onSurface` — not a mechanical invert of the dark values; light mode gets its own tonal pass same as the current Onyx system does.
- **Accent**: one brick-red (`primary`) carries the margin rule, active/expanded row indicator, FAB, and primary buttons — replacing `OnyxPrimary`'s blue. No secondary/tertiary brand hues; `secondary`/`tertiary` roles collapse to graphite-gray tonal variants rather than distinct hues, keeping the palette Restrained (neutrals + one accent).
- **Status color** (functional, not brand): `PlateauStatus` (PROGRESSING/PLATEAUING/REGRESSING, already a real domain enum) and PR moments get a small, separate signal layer — muted moss-green / amber / the existing `error` red — distinct from the brand's ink-red so a plateau flag never gets confused with the primary accent. This is the one deliberate exception to "one accent," scoped to state legibility only, same as Material's own `error` role sitting outside a brand palette.
- Delete the unused `HabitColorPresets`/`HabitSwatch`/`HabitSwatchFamily`/`HabitColorPalette`/`HabitPaletteHexes` block (confirmed zero references outside `Color.kt` — leftover HeatP habit-tracker scaffolding, never wired into TimeGo).

## Section 2 — Typography

- **New tabular monospace face** for every logged number: set weight/reps/seconds/distance/dates, `StatTile` PR values, strength-curve/sparkline axis labels, heatmap legend values. Needs sourcing and bundling into `res/font/` (e.g. JetBrains Mono, Apache-2.0 — confirm license before bundling; no monospace font currently ships in the app). This is a structural choice, not decoration: tabular figures are what make a column of logged sets actually align like a ledger.
- **Manrope stays** for body text, labels, buttons, and exercise names — it's already a solid workhorse humanist sans and Operate-mode surfaces are well served by keeping UI chrome on it; no reason to replace what wasn't the complaint.
- **Fraunces is retired entirely** — it never carried real identity (only two call sites since Aug 10) and a warm serif display face works against the ledger's tabular, ruled character. Remove `FrauncesFamily`, `FrauncesStatValue`, `FrauncesEmphasis`, the `fraunces_variable.ttf` bundled font, and every `displayLarge`/`displayMedium`/`displaySmall`/`headlineLarge` reference to it in `Type.kt` (falls back to the new mono or Manrope depending on role — display roles aren't currently called anywhere in the app per the Aug 10 spec's own finding, so this is safe).

## Section 3 — Shape & structural language

- **Corner radii flatten hard.** Current `TimeGoShapes` (14–28dp across small/medium/large) reads soft and app-generic — wrong for a ruled page. New scale stays near-rectangular: small ≈ 2–4dp, medium ≈ 4dp, large ≈ 6dp. `extraLarge` (bottom sheets) keeps enough radius to not clip content per the existing code comment, but pulled down from 28dp.
- **Ruled lines over elevated cards.** `ExerciseCard`'s current pattern (elevated `Card` + 3dp accent-colored left bar) is replaced by a hairline horizontal rule between entries (ledger row dividers) plus the red accent reserved for the active/expanded row only, not every row — right now every card carries its category color permanently, which is closer to color-coded chrome than a ledger's restraint.
- Card elevation (2dp, added Aug 10) is dropped in favor of the rule-line separation; flat ruled surfaces are the point of the direction, not another elevation tier.

## Section 4 — Motion

Named complaint: existing motion (`AnimatedExpand`'s linear/ease tweens, nav crossfade, `AnimatedContent` fade+scale on Progress) "feels generic/flat" — this section exists specifically to answer that, not to add more animation.

- **`AnimatedExpand` rewrite**: spring-based expand (`spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)` or tuned equivalent) replacing the linear/ease-in-out tween, paired with the red margin rule animating in alongside (a short rule-draw, not a separate fade) — this is the concrete "ink-reveal" moment named in the direction contract.
- **Bottom-nav transition**: replace the plain crossfade with a Material shared-axis-X transition (slide + fade together, per `android.md`'s "Material motion patterns" guidance) — reads as one page replacing another, not a dissolve.
- **Progress screen `AnimatedContent`** (PR tiles, strength-curve swap on wheel-picker selection): move from tween fade+scale to spring-based content transform, consistent with the expand rewrite so the app has one motion character, not two.
- Respect the system "Remove animations" setting throughout (already implicit via Compose's `AnimatedVisibility`/`AnimatedContent`, but worth a explicit check during implementation since spring specs sometimes get missed here).

## Section 5 — Log screen

- `ExerciseCard`/`ExerciseRowHeader` restructured per Section 3: hairline rule instead of elevated card, red rule appears only when a row is expanded/active.
- Weight/reps/seconds/distance input fields and suggestion text switch to the new mono face.
- Session-type filter chips (`FilterChip` row) keep Material's chip component (native affordance, not replaced) but re-themed through the new color roles.

## Section 6 — Progress screen

- `StatTile` PR values move from `FrauncesStatValue` to the new mono ledger-figure treatment.
- Strength-curve/sparkline chart, muscle heatmap, and radar chart keep their existing math and structure untouched — only their color roles (line/fill colors, heatmap scale stops) and any axis/legend text move onto the new palette and mono type. `heatStopHexes()`'s 5-stop scale gets re-picked within the new palette family, not redesigned.
- `HorizontalWheelPicker` re-themed (rule-line ticks instead of the current pill/rounded selection treatment) — scope stays the picker's existing two selectors, no rollout to new fields (still out of scope per prior sessions).

## Section 7 — Routines screen

- Routine names move from `FrauncesEmphasis` to the mono ledger treatment (short, identity-bearing values read naturally as a ledger entry number/label, same logic that put PR values in mono).
- Routine list restructured onto ruled-row dividers matching Log, replacing its current card treatment for consistency across the app (this was the whole-app chrome complaint, not a Log-only fix).

## Application order & verification

Foundation first, since every screen depends on it: **Section 1 (color) → Section 2 (type, incl. new font asset) → Section 3 (shape) → Section 4 (motion)**, verified with `./gradlew assembleDebug` after each since these are shared tokens with no domain logic attached. Then screen-by-screen: **Log → Progress → Routines**, same order as the Aug 10 pass, each verified individually.

No new domain logic anywhere in this pass — pure UI/theme. No new unit tests needed; existing `testDebugUnitTest` suite must stay green throughout (a broken build here would mean a token rename or import error, not a logic regression). No proactive screenshots — per established project preference, build + `installDebug` and hand off; the user verifies on their Galaxy S23 Ultra and follow-up fixes come from that real feedback. Branch: `training-ledger-redesign`, inline execution task-by-task, matching every prior TimeGo session.

## Out of scope

- Any domain/data-layer change — `PlateauStatus`, suggesters, muscle weighting, Room schema all untouched; this pass only changes what those values look like on screen.
- Chart/diagram underlying logic (`RadarChart`, `MuscleBodyDiagram`, `SparklineChart`, `HeatmapGrid` math) — restyled colors/type only, no new visualizations.
- `HorizontalWheelPicker` rollout beyond its current two selectors (standing "other not now" scope decision).
- Bottom-nav icon set, navigation structure, or screen count — Log/Progress/Routines stays the app's shape.
- Any new feature surface (rest timers, GPS routes, etc.) — this is a restyle of what exists, not new scope.
