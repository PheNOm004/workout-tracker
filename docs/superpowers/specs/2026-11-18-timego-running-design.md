# TimeGo running: design

Status: draft for owner review, 2026-11-18. Nothing here is built. Decisions marked **Owner** were made by the owner in conversation; decisions marked **Builder's call** are left to whoever plans and builds it.

## Intent

The owner wants to record real runs in TimeGo, outdoors and on a treadmill, and at the end of a run see the route they took, their pace and splits, and an estimate of calories burned. They carry both a phone and (soon) a Fitbit when they run, and later want the same feature to work with watches from any brand, Samsung to Google.

Why: TimeGo is a fully local gym logger with no account, no server and no subscription. A running feature should keep that promise. A subscription to a watch ecosystem "defeats the whole purpose of creating this" (owner).

## Goals

- Record an outdoor run with the phone's GPS and show the route, distance, time, pace, per-km splits, elevation gain and estimated calories when it ends.
- Record a treadmill run from time and the distance shown on the treadmill display.
- Keep working with no network, no account and no subscription.
- Later, take heart rate, calories and finished sessions from a watch, from any brand, without a per-brand integration.
- Fit the existing app: runs appear in history and on the attendance heatmap, in the Plate Colours look (see `DESIGN.md`).

## Non-goals (first version)

- Social features or sharing runs.
- Training plans, goals, race targets or coaching.
- Audio cues (spoken splits).
- Real map tiles. The route is drawn by TimeGo as a line. Tiles can be added later as an optional layer.
- Heart rate zones and live heart rate on screen (these wait for the watch phase).
- Any account, subscription or server.
- Apple Watch (cannot work with an Android app).

## Anti-goals

- A run must never be lost because the phone locked, the app was backgrounded, or the process was killed.
- A route must never leave the phone without the owner turning that on.
- No "pro" tier, paywall or recurring cost anywhere in the flow, including the recommended watch.

## Decisions (owner)

### Source of truth per number
When a phone and a watch both record the same run:
- Route, distance and pace outdoors: phone GPS.
- Heart rate and calories: the watch when it has data, otherwise TimeGo's own estimate from pace, time and body weight.
- Watches also measure distance (GPS on models that have it, a step-and-stride estimate otherwise). Outdoors the phone's GPS distance wins; on a run with no phone, the watch's GPS distance is used.
- Treadmill distance and speed: the owner's entry from the treadmill display wins. A watch's stride-based estimate only pre-fills the field for the owner to correct.
- TimeGo shows the winning number and keeps the other one visible. Each run records which source each number came from.

### How a run is started
A "Start run" card on the Log landing screen next to "Start freeform workout", offering Outdoor (GPS) and Treadmill. A run is its own session (not a cardio exercise inside a lifting session). It appears in history and on the attendance heatmap. Its effect on the muscle balance chart is small, so it does not distort it.

### Outdoor run behaviour
- Start, Pause and Stop in the app and in the lock-screen notification, usable without unlocking the phone.
- Auto-pause when the user stops moving, **on by default**, with a setting to turn it off. Pace and splits use moving time only. The summary shows moving time and total time.

### Treadmill run entry
Time and the distance shown on the treadmill display. Average incline is optional. No per-minute speed entry.

### Privacy
Routes stay on the phone by default. A separate setting, off by default, includes routes in the optional cloud backup. If sharing or export is ever added, trim the first and last 200 m of the route. The local phone backup file contains everything.

### Finished-run screen
Top to bottom: the route as a line coloured by pace with start and finish markers; distance, moving time, total time, average pace; per-km splits with fastest and slowest highlighted; elevation gain if the phone provides usable altitude; calories labelled as an estimate; average heart rate and a short graph only when watch data exists for that run; one "vs your last run" line for distance and pace.

### Watches: "all brands" means two common routes
- Health Connect (Android's on-phone health store) for finished sessions, heart rate and calories. Samsung Health and the Fitbit/Google Health app write into it.
- The standard Bluetooth Heart Rate Profile for live heart rate during a run, from any watch or strap that supports it.
- No per-brand integrations. A brand-specific connection is added later only if a brand is missing something important.
- Each run stores its data sources, so the rest of the app does not care which brand a watch is.

### Phasing
1. Phone only: outdoor GPS runs, treadmill runs, own sessions, finished-run screen, auto-pause, local storage.
2. Watches: Health Connect read, Bluetooth heart rate, source-aware merge. Needs a real watch to test.
3. Later and optional: map tiles, routes in cloud backup, a brand-specific connection if needed.

### Recommended test watch (owner's purchase)
A Fitbit Charge 6: shares heart rate over Bluetooth (per Google's help page), works without Fitbit Premium for heart rate, workouts and GPS (per reviews), and syncs through the Fitbit app to Health Connect on Android. Typical Australian price about A$185, past lows A$138 to A$156; no confirmed current deal. A Pixel Watch 3 is the alternative if it falls to around A$350 and the owner wants to run without the phone. Avoid no-name "screenless" bands: no Fitbit app, no known Health Connect path, unknown accuracy.

## Constraints and facts the builder should know

- Stack: Kotlin, Jetpack Compose, Room (database version 17), minSdk 26, targetSdk 37. The app has no location permission, no foreground service and no Health Connect code today.
- The existing manual cardio row (`CardioLogRow`) logs minutes and distance and shows pace and a MET-based calorie estimate. Distance is required for cardio exercises (as of the set-entry redesign).
- Design system: `DESIGN.md` (Plate Colours: red only for actions, blue informational, green progress; no gradients or shadows; JetBrains Mono for logged numbers). Pace colouring of the route needs a ramp that works in light and dark and meets contrast.
- The Fitbit Web API is shut down; its replacement is the Google Health API (cloud, needs Google sign-in). This design uses Health Connect instead, so there is no sign-in. Google's migration page is the authority on dates.
- Health Connect exercise routes: reading a route needs a route permission plus an exercise-session permission, and background reads of another app's route can require the user to confirm again. A third-party note says route permission cannot be pre-granted in Android settings; this is unconfirmed.
- It is not confirmed which of a Fitbit's data (heart rate, calories, route) reaches Health Connect, or whether Google's move of Fitbit accounts to Google accounts affects the sync or becomes paid. Test with the real watch on day one of phase 2.
- Sources for the above: Google Health API migration overview; Android Health Connect workout and exercise-route guides; Fitbit help pages on Bluetooth heart rate and Health Connect; Wareable and DC Rainmaker Charge 6 reviews.

## Left to the builder (Builder's call)

- GPS sampling interval, accuracy settings and battery trade-offs; filtering of bad fixes; how "stopped moving" is detected for auto-pause.
- How a route and its per-point data are stored (Room tables, a compressed blob, or a file), and the Room migration to version 18. Storage must survive a process kill mid-run (see anti-goals).
- Foreground service and notification design; the permission flow (foreground location, notifications) and what the app does when permission is refused or revoked mid-run.
- The calorie formula from pace, time and body weight, and what is shown when no body weight is recorded.
- Elevation handling and when to hide it.
- How the "vs your last run" line picks its comparison run.
- Test strategy, including how to test GPS logic without running outdoors.

## Calls made while writing this (Builder's call, from the builder check)

- Phase 2 includes live heart rate on screen (the non-goal above applies to the first version only). The phase 1 foreground service must be built so a connected-device (Bluetooth) heart rate stream can be added without reworking it.
- A watch session and a phone session are treated as the same run when their time ranges overlap; if Health Connect provides an external session id, store it and match on that first.
- Phase 1 schema should make route and distance nullable, tag each number with its source, and keep an external-id field, so phase 2 needs no second reshaping.

## Decision added after the builder check (owner)

A run recorded only on a watch (no phone, so no phone route) is imported as a TimeGo run. Its route is empty or taken from the watch, and its source is recorded as the watch. It appears in history like any other run.

## Open questions for the owner before phase 2

- Which watch is bought and when (see above). Nothing else is open.

- Which watch is bought and when. Phase 2 cannot be finished without it.
- Whether the Health Connect sync from the Fitbit app turns out to carry heart rate, calories and a route.

## Success

- An outdoor run records through a locked screen and ends in a finished-run screen with the route, splits and calories, with no account and no network.
- A treadmill run takes under 30 seconds to enter.
- Nothing about the feature costs money.
- A second watch brand (for example a Galaxy Watch) works through Health Connect or Bluetooth heart rate with no brand-specific code.
