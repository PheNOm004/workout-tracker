# TimeGo — Motion Polish (design)

**Date**: 2026-08-10
**Status**: approved, not yet implemented
**Context**: Triggered by installing the `ui-ux-pro-max` Claude Code plugin (a searchable local UI/UX guideline database, no network calls, MIT-style open source project by nextlevelbuilder) and running its design-system + UX-guideline queries against TimeGo. The tool's generic fitness-app style recommendation (vibrant orange/green, Barlow Condensed) was explicitly declined — TimeGo keeps its existing Onyx dark identity, finished in the visual-identity-pass session (2026-08-10). This session applies only the tool's animation/motion guidance, cross-checked against TimeGo's actual code, not a wholesale redesign.

## What the tool's checks actually found

Running `--domain ux` queries for animation and touch-interaction guidelines against TimeGo's real screens surfaced two concrete gaps: no haptic feedback anywhere (declined by the user — animation only, no haptics this session), and every expand/collapse state change (`if (expanded) { ... }`) and screen transition is an instant cut with no animation. `TimeGoNavHost.kt` was checked against the tool's safe-area-inset guideline and found already correct (Scaffold's `innerPadding` is applied to the NavHost content) — no fix needed there.

## Scope

Per the tool's own "1-2 key elements per view maximum" anti-pattern (excessive motion), four targeted additions, not a blanket animate-everything pass:

1. **Row/section expand-collapse** — `LogScreen.kt`'s three row types (`StrengthLogRow`, `CardioLogRow`, `HoldLogRow`) and `ExerciseListSections.kt`'s category/muscle-group headers currently gate their content with a bare `if (expanded) { ... }`. Wrap each in `AnimatedVisibility` using a shared easing convention (from the tool's motion guidance): enter with `expandVertically` + `fadeIn` over 250ms `LinearOutSlowInEasing` (ease-out), exit with `shrinkVertically` + `fadeOut` over 150ms `FastOutLinearInEasing` (ease-in, faster than enter — standard "exit-faster-than-enter" motion principle).
2. **Bottom-nav screen crossfade** — `TimeGoNavHost.kt`'s three `composable("log"/"progress"/"routines") { ... }` calls currently transition with Navigation Compose's default (an instant cut, since no transition is specified). Add `enterTransition = { fadeIn(tween(200)) }` and `exitTransition = { fadeOut(tween(150)) }` to each.
3. **Progress PR/curve content swap** — `ProgressScreen.kt`'s Personal Records `StatTile` row and the Strength Curve's `SparklineChart` currently hard-cut to new data the instant the wheel picker's selection changes. Wrap both in `AnimatedContent` keyed on the selected exercise/muscle-group, with a fade+slight-scale transition between old and new content.

## Out of scope

- Haptic feedback (explicitly declined this session).
- Any color/typography/layout changes — Onyx identity stays as finished in the visual-identity-pass session.
- Canvas-drawn content (the muscle body diagram, radar chart) — animating per-shape heat-color transitions would need `animateColorAsState` wired through the Canvas draw loop, a meaningfully bigger change than this session's scope; not requested.

## Verification

Pure UI/motion change, no domain logic — same discipline as the visual-identity pass: build-verify (`assembleDebug` + existing test suite staying green), no new unit tests needed. On-device verification (phone is connected): expand/collapse a Log exercise row and a library section header, switch bottom-nav tabs, and switch the Personal Records wheel picker's selected exercise — confirm each transitions smoothly rather than cutting instantly.
