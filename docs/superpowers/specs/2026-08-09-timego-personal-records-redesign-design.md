# TimeGo — Personal Records Redesign (Design)

## Context

Second of a 3-part sequenced frontend effort (muscle groups → **Personal Records redesign** → full visual-identity pass), a deferred idea from the Update 1.1 session: "Personal Records section redesign: more presentable cards + a horizontal looping scrollwheel (matching the existing `HorizontalWheelPicker` pattern)."

Currently (`ProgressScreen.kt:111-138`), PRs render as a plain `LazyColumn` of cards — one card per exercise that has at least one PR, each listing up to 3 plain text lines ("Heaviest Weight: 80.0kg on 2026-08-05") with a raw `LocalDate.toString()` date. The core problem: once several exercises have PRs, this list gets long to scroll through.

## Scope

Replace the vertical list with a `HorizontalWheelPicker` (the same component the Progress screen's strength-curve exercise/muscle-group selector already uses) to pick one exercise at a time, and a single card below it showing that exercise's PRs as `StatTile`s (the same tile component the Workouts/Duration/Volume/Sets row already uses) instead of plain text lines.

Out of scope: no `ProgressViewModel` changes, no domain-logic changes (`personalRecords()` in `ProgressMath.kt` is untouched — this is purely a `ProgressScreen.kt` UI change), no changes to how PRs are computed or ranked.

## Design

**Wheel population**: exercises with ≥1 PR, sorted alphabetically by name (`records.groupBy { it.exerciseId }` already exists; filtered to exercises present in `exercises` — same lookup already done today — then sorted by name for a stable, predictable wheel order, since the current `groupBy` iteration order is effectively arbitrary).

**Selection state**: local `remember { mutableStateOf<Long?>(null) }` for the selected exercise ID, living in `ProgressScreen` — mirrors the existing strength-curve selector's pattern (`selectedIndex = list.indexOfFirst { ... }.coerceAtLeast(0)`), not the ViewModel, since `records` already contains every PR up front and no new data fetching is needed.

**Selected-exercise card**: exercise name as a title, then a `Row` of 3 `StatTile`s (Heaviest Weight, Most Reps, Best Volume) using the existing `formatRecordValue()` for the value text. Each tile also shows the date the PR was achieved, formatted `"MMM d"` (e.g. "Aug 9") via `DateTimeFormatter.ofPattern("MMM d")`, instead of today's raw `LocalDate` string.

**`StatTile` change**: add one new optional parameter, `caption: String? = null`, rendered as a third small `Text` line (labelSmall, onSurfaceVariant) below the value only when non-null. The 4 existing call sites (Workouts/Duration/Volume/Sets) pass nothing and are visually unchanged.

**Empty state**: unchanged — "No personal records yet -- log a few sets to see them here." shown when `records` is empty, no wheel rendered in that case.

## Testing

No new domain logic to unit test (this is a pure Compose UI reshuffle around existing data/components). Verification is on-device: confirm the wheel scrolls through exercises with PRs, the selected card's 3 tiles show correct values, dates are readable, and the empty state still renders correctly on a exercise with no PRs.
