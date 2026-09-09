# TimeGo release motion and transition audit

Date: 2026-09-09. Reviewed application code: `2449baf` on `master`.

**Baseline verdict (before repair pass): not ready for final release acceptance.** A navigation round trip lost a running timer. Several modal, content and layout transitions also violated the intended continuity. Passing compilation and domain tests did not cover these failures.

This document records the baseline audit and the repair work that followed. Existing untracked body-map drafts were preserved.

## Repair pass (2026-09-09)

The release-hardening fixes now applied are:

- Timer ownership moved to `LogViewModel` with `SavedStateHandle`; active timer, search and expanded exercise state survive the active-workout/landing round trip and timer controls render the last valid outgoing phase.
- Root tabs use interruption-safe `AnimatedContent` with `SaveableStateHolder`, so only visible/outgoing routes are composed while state remains restorable.
- Settings actions and backup-result dialogs await exit animation completion before unmounting or launching the next action. Dialog dismissal no longer has a fixed 350 ms cutoff.
- Loading states now use explicit shimmer skeletons for Log, Progress and Routines instead of showing actionable empty content while repositories hydrate.
- Exercise expansion uses `BringIntoViewRequester` after the next frame and reserves additional list tail space; fixed viewport-height/two-stage delay guesses were removed.
- Strength-curve updates crossfade by stable data content, body-metric rows animate size, custom exercise creation exposes required-field guidance and disables Add until valid, and bottom tabs/body maps expose accessibility semantics.
- System-bar appearance is explicitly dark and transparent to match the forced-dark theme, including navigation-bar contrast handling.
- Active logging is now a focused current-session surface; the full catalogue, search, order/filter
  controls and custom-exercise action move to an on-demand picker. The picker keeps its catalogue
  data warm in the ViewModel, renders explicit A-Z section dividers, and exposes a draggable thumb
  for the bounded alphabetical list. The landing Recommended Focus panel remains intact; the
  active logger does not duplicate it. Strength/calisthenics Quick Add is contextual and does not
  surface warm-up/cardio suggestions in a strength session.

This pass changes application Kotlin; it does not authorize installation or UI automation on the primary S23. Physical-device acceptance remains user-owned.

## Evidence and scope

- Inspected all three root screens, retained navigation, their ViewModel subscription gates, shared dialogs/sheets, timers, expansion/scroll helpers, active-workout feedback, charts, body-map popup, picker, surface and motion tokens.
- Built the reviewed debug APK and installed it on a separate API 30 Pixel 4 emulator, `emulator-5556`, AVD `timego_motion_audit_20260909`. Used UI-tree-derived targets for interaction.
- Runtime checks: Log/Progress/Routines, Settings to History, dialog open/close at animator scales 0/1/10, new-routine cancel, invalid custom-exercise submission, Training/Body switch, 18-week/year heatmap toggle, exercise expansion, and running timer -> landing -> continue.
- The primary S23 was not installed, launched, navigated, screenshotted or instrumented. A first smoke check used shared emulator-5554, but inspection stopped when it switched to HeatP; substantive reproductions below use the dedicated instance. That initial shared-emulator sample is not release evidence.
- All screenshots in the adjacent evidence directory are from the dedicated emulator. The only workout created for the reproduction was an empty emulator session; no user workout database was copied.

### Verification result

`testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --console=plain`: **BUILD SUCCESSFUL** after the repair pass and focused-logger refactor. JVM results: **304 tests, 0 failures, 0 errors**. Lint: **0 errors, 6 warnings**. The Android-test APK also assembles successfully. No motion regression suite was added. Dedicated-emulator crash buffer was empty after the earlier exercised flows; physical-device acceptance remains user-owned.

The lint warnings include screen-height estimation in dialog/expansion code and non-lambda animated navigation offsets. Dependency-version notifications are not a reason to mix upgrades into a motion fix.

## Findings

P1 = release blocker involving lost interaction state. P2 = visible behavior/continuity defect or material release-quality gap. P3 = lower-priority consistency/documentation work. Runtime reproduction and code evidence are distinguished below.

### M01 — P1: landing round trip discards a running timer

**Runtime reproduced.** Start a freeform workout, search for Dead Hang, expand it, start the timer, return to the landing page, then choose Continue Workout. Before navigation the timer showed **7s**. After returning and reopening Dead Hang it showed **Start timer**. The active workout itself remained open. The search and expanded exercise selection also reset.

Cause: `LogScreen.kt:158-185` switches between two separately composed branches. `LoggingContent` owns local search/expansion state at `LogScreen.kt:603-607`; `TimerControls.kt:91-92` owns the timer origin and phase with `remember`. Once the outgoing logging branch leaves composition, that state is discarded. The same ownership makes other removals of the timer subtree risky; collapsing/filtering and recreation need explicit tests.

Fix requirement: session/exercise-scoped timer and draft state must survive this navigation. Do not repair this by keeping every hidden subtree actively polling. Define explicit cancellation separately from leaving a view.

Acceptance: countdown and running timers retain the correct elapsed duration after landing/continue, root-tab navigation, filtering/collapse, background/resume and activity recreation. No duplicate or accidental set is logged. Search, selected exercise and unsaved values remain coherent.

Evidence: [running at 7s](2026-09-09-motion-evidence/timer-running-before-landing.png), [reset after return](2026-09-09-motion-evidence/timer-reset-after-landing.png).

### M02 — P2: retained tabs are hidden visually but remain active

**Code confirmed; popup reproduction outstanding.** `TimeGoNavHost.kt:128-189` composes all destinations permanently and hides inactive ones with alpha and translation. It supplies no per-tab lifecycle boundary. Each screen continues using the activity lifecycle for `collectAsStateWithLifecycle`. The subscriber-based suspension gates in `LogViewModel.kt:218`, `ProgressViewModel.kt:139` and `RoutinesViewModel.kt:80` therefore cannot represent tab visibility. `TimerControls.kt:101` also uses the shared STARTED lifecycle.

The Progress muscle readout is a separate `Popup` window (`MuscleBodyDiagram.kt:249-273`), controlled by a remembered pinned selection, not route visibility. Hiding its ancestor graphics layer cannot be assumed to dismiss this separate window. Pin -> switch root tab must be tested specifically. Similar asynchronous modal state can surface from hidden destinations.

Fix requirement: preserve state while explicitly controlling visible lifecycle, effects and overlay ownership. Test that hidden tabs suspend reactive work and cannot display overlays over another destination.

### M03 — P2: rapid root-tab changes reset the transition mid-flight

**Code confirmed; frame-by-frame runtime measurement outstanding.** `TimeGoNavHost.kt:111-124` cancels the previous effect, replaces `previousRoute` with the last target and snaps shared progress to zero. In Log -> Progress -> Routines, the partially visible Progress screen becomes a fully opaque outgoing screen at progress zero, while the original Log layer is dropped. Reversing direction has the same discontinuity.

Fix requirement: retarget from the displayed state or use an interruption-aware transition with retained saveable state. The indicator and screen must agree during reversal and repeated taps.

Acceptance: 50-150 ms alternating taps, direct Log/Routines jumps, Back during a switch, and repeated selection of the active tab produce no flash, origin reset, stuck input layer or mismatched indicator.

### M04 — P2: programmatic modal transitions bypass exit animation

**Settings -> History reproduced; other paths code confirmed.** `RoutinesScreen.kt:115-125` removes Settings immediately before launching Export, Restore or History. `SettingsBottomSheet` has a sheet state, but the callbacks never await its hide transition. The outgoing sheet disappears while the destination starts entering.

The backup-result OK handler calls both `dismiss()` and `clearBackupResult()` immediately (`RoutinesScreen.kt:89-93`), unmounting the dialog before its graceful dismissal completes. End-session confirmation also starts the parent session transition immediately (`LogScreen.kt:631-636`); verify coordination with its outgoing dialog instead of assuming they finish together.

Fix requirement: complete dismissal before navigation/unmount, and allow completion to execute only once. Avoid coupling destructive work to repeated button taps during the exit.

Acceptance: Settings -> History/Export/Restore, backup-result OK, cancel, scrim, Back and end-session all have intentional entry and exit without stacked scrims or abrupt removal.

Evidence: [Settings removed while History begins entering](2026-09-09-motion-evidence/settings-to-history-mid-transition.png), captured with 10x animations to expose ordering.

### M05 — P2: dialog timing and scrim do not share a complete transition

**Slow dismissal reproduced; scrim initialization code confirmed.** `TimeGoDialog.kt:101` forcibly finishes dismissal after 350 ms regardless of the animator duration scale. At 10x, a nominal 240 ms exit needs about 2.4 s, but the window is removed early. At normal speed the scrim has a 260 ms exit while the content completes at 240 ms, so awaiting only content idle can also cut off the tail.

On entry, `transitionState.targetState` is already true before `animateFloatAsState` is first composed (`TimeGoDialog.kt:85-112`). Its initial scrim target is already 0.65; there is no explicit zero-alpha initial state for this independent animation. Content fades in over an already darkened background. Repeated platform dismiss can additionally bypass the exit at lines 116-120.

Fix requirement: drive panel and scrim through one coherent transition and unmount when all exit work is complete. Any escape hatch must not invalidate duration-scale behavior.

Acceptance: panel and scrim complete correctly at 0x, 1x, 2x and 10x, including repeated Back and dismissal during entry. The tested 0x open/close paths did work; this audit does not claim that Compose animations universally ignore reduced-motion settings.

Evidence: [dialog already gone during scaled exit](2026-09-09-motion-evidence/dialog-exit-10x.png).

### M06 — P2: loading is rendered as empty content, with no coherent handoff

**Code confirmed; slow-hydration capture outstanding.** `LogScreen.kt:145-148` renders Loading and NoActiveSession using the same actionable landing UI, but treats them as different outer transition targets. An active-session launch can therefore pass through the wrong landing state. Routines collects `isHydrated` (`RoutinesScreen.kt:62`) but never uses it to distinguish loading from no routines. Progress masks only four statistic values (`ProgressScreen.kt:232-235`); its other sections render initial empty data.

Placeholder components exist but are not wired into those screens. Their presence does not establish loading coverage.

Fix requirement: one explicit loading-to-content/empty handoff per screen. Preserve structural positions and do not offer an action based on an unknown session state.

Acceptance: cold launch with existing active session, many routines/history records and delayed repository emissions never flashes a misleading empty state or double-animates equivalent landing content.

### M07 — P2: dynamic content updates still snap or reflow abruptly

**Code confirmed.** Existing animations cover some transitions, but not these changes:

| Surface | Gap and source |
|---|---|
| Landing recommendation | Suggested exercise/note/exhaustion content updates directly; exhausting alternatives removes a button and changes height (`LogScreen.kt:398-429`). Flexible-routine selection likewise directly replaces title, metadata and action text. |
| Current-session summary | The first section expands, counters animate and badges scale, but later exercise rows and wrapped badge rows change Column/FlowRow size immediately (`ActiveWorkoutSection.kt:112-187`). |
| Strength curves | `AnimatedStrengthCurve` keys by empty/nonempty (`ProgressScreen.kt:624`). Updates within the same populated selection retain that key, and `SparklineChart` draws the new points directly. Existing exercise/muscle selection transitions should be preserved. |
| Body metrics | Weight Trend and Metric History appear conditionally, BMI text swaps directly, and metric items lack placement/appearance animation (`ProgressScreen.kt:445-554`). |
| Expanded exercise surface | `hero = expanded` changes the surface deck/elevation immediately (`LogScreen.kt:932`, `SurfaceCard.kt:41-44`) while its contents expand over 280 ms. |
| Year heatmap labels | The year subtree is newly mounted with inner `AnimatedVisibility` already true (`HeatmapGrid.kt:107-136`); an enter spec alone does not supply a hidden initial state. Check first entry separately from exit. |

Fix requirement: choose a small, consistent content/size transition per affected surface, preserve outgoing data until exit completes, and use stable item identity. Do not add decorative animation to every value by default.

Acceptance: exercise rotation/exhaustion, first and subsequent set rows, badge wrap, first body metric, subsequent metric, populated curve refresh, and first/repeated year toggle preserve visual context without jumps.

### M08 — P2: outgoing timer content reads the new phase

**Code confirmed.** The AnimatedContent target is only the IDLE/COUNTDOWN/RUNNING enum. Inside each outgoing branch, rendering reads the live outer `phase` (`TimerControls.kt:176`, `209`). After countdown -> running, the outgoing countdown cast becomes null and renders `Starting in 0s...`. After stopping, the outgoing running branch can render zero until its exit completes.

Fix requirement: transition an immutable display snapshot or retain the last valid outgoing phase/value. Keep wall-clock elapsed-time calculation separate from animated presentation.

Acceptance: slow animations show the correct last countdown/running value throughout exit, with no fabricated zero flash or repeated logging action. Timer/manual mode switches should receive an intentional transition rather than the current raw branch swap.

### M09 — P2: expansion does not guarantee unobstructed actions

**FAB overlap reproduced; scroll timing risk code confirmed.** The running-timer screenshot shows Add Exercise covering part of Stop & Log on the standard emulator viewport. Exercise centering estimates viewport height as screen height minus 140 dp, waits 100 ms, scrolls, waits another 200 ms, then scrolls again (`LogScreen.kt:902-925`). It does not derive the unobscured area from the actual list, FAB and IME. Category/subcategory expansion also uses fixed delay pairs in `ExerciseListSections.kt:425-476`.

Fix requirement: use measured viewport/occlusion bounds and animation/layout completion for scrolling. Reserve space for the FAB or reposition it so logging controls stay fully exposed. Avoid a two-stage correction that looks like a second jump.

Acceptance: long exercise cards, keyboard open/closed, font scaling, small screens and repeated expansion leave the primary action fully visible and reachable.

Evidence: [Stop & Log partly covered](2026-09-09-motion-evidence/timer-running-before-landing.png).

### M10 — P2: custom-exercise Add silently ignores invalid input

**Runtime reproduced.** Open Add Custom Exercise and tap Add with no name or muscle groups. Add looks enabled and responds to a tap, but nothing explains the missing input. `AddExerciseDialog.kt:40-45` only guards the callback; it supplies neither disabled state nor validation feedback. The routine form already demonstrates a disabled-action pattern.

Fix requirement: explicit missing-field feedback and consistent enabled state. Motion feedback must communicate a real outcome, not imply a save succeeded.

Evidence: [invalid Add with no feedback](2026-09-09-motion-evidence/invalid-add-no-feedback.png).

### M11 — P2: system bars do not follow the forced-dark app

**Runtime reproduced on API 30 with system light mode.** Status-bar icons are black against TimeGo's dark background and the three-button navigation area is light. Dialog presentation changes icon treatment, making the transition especially conspicuous.

`MainActivity.kt:17` uses default `enableEdgeToEdge()`, while `TimeGoTheme` defaults to dark unconditionally (`Theme.kt:116`). Their appearance decisions disagree.

Fix requirement: set system-bar appearance consistently with the actual app theme and verify activity/dialog/sheet windows, gesture navigation and three-button navigation on supported APIs.

Evidence: [dark status icons/light system navigation](2026-09-09-motion-evidence/landing.png).

### M12 — P2: interaction state is not fully represented to accessibility

**Code/UI-tree confirmed; TalkBack walkthrough outstanding.** Bottom tabs use generic clickable containers and duplicate icon/text labels without explicit selected-tab semantics (`TimeGoNavHost.kt:284-326`). The body-map canvases expose pointer gestures but no named muscle actions (`MuscleBodyDiagram.kt:199-237`); the dedicated emulator's accessibility tree contains no muscle targets. A smooth visual popup does not make that feature operable for non-touch users.

Fix requirement: meaningful selected/tab roles, nonduplicated announcements, and an accessible equivalent for muscle inspection. Verify focus return after dismissals and focus ownership during transitions.

### M13 — P3: the documented visual/motion contract has drifted

**Code/vault mismatch confirmed.** The vault's UI note still presents coral Night Training Console as current. `Theme.kt` identifies the later brass/gunmetal Engine-Room Gauge Panel. Navigation actually uses 260 ms while the note says 300 ms; the note describes the scrim/exit behavior as complete despite M04-M05. Do not resolve this by redesigning the application to match the stale note.

Fix requirement: reconcile the approved design record against implementation history before choosing motion tokens for fixes. Retain intentional tactile springs, chart interpolation and subtle detail; consolidate accidental exceptions. The standalone dial-sweep token is not proof that a dial still exists.

## Existing behavior to preserve

- Root navigation retains screen state and scroll positions across ordinary tab switches; preserve that value while fixing lifecycle and interruption handling.
- Training/Body and exercise/muscle selection already use explicit directional/content transitions.
- Radar interpolation starts from the displayed intermediate polygon on interruption. Body-map intensity values already animate.
- Muscle readout keeps its last content mounted through its own fade/scale exit.
- WorkoutHistory and PeriodBreakdown Close buttons await sheet hiding, unlike Settings navigation callbacks.
- First-session-summary appearance, count transitions, set-badge scale feedback, tactile buttons and expansion clipping already exist.
- Heatmap yearly scrolling has an explicit return glide and should not be replaced with an unconditional jump.

## Required repair order and release acceptance

1. Fix timer/draft ownership and hidden-route lifecycle/overlay ownership together. Add focused regression coverage for M01 and pinned-popup tab changes.
2. Fix interruption-safe navigation and unified modal dismissal, then validate normal, disabled and scaled animations.
3. Fix viewport/FAB/IME behavior and state-snapshot rendering for timers and dynamic content.
4. Complete loading/empty/error/validation/accessibility states and system-bar consistency.
5. Reconcile the design note, run JVM/lint/debug/release plus targeted emulator interaction checks, then perform device acceptance with separately approved S23 installation.

The current audit does not certify populated-chart motion, cold-start frame pacing, pinned-popup tab changes, TalkBack, rotation, split-screen, all font scales or physical-device smoothness. Those are explicit acceptance gaps, not implied passes. Frame-time profiling and a recent-API emulator/physical device are still needed for final performance acceptance. A complete public-release review also needs separate packaging, recovery, privacy and distribution checks; this report focuses on motion and directly encountered UI shortcomings.
