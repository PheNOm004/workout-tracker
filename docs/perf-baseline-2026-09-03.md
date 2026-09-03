# TimeGo performance baseline — 2026-09-03

Captured before the full three-axis optimisation pass so post-pass numbers are comparable.
Continues the measurement style of the 2026-08-25 and 2026-09-02 passes.

## Rig

- **Device:** AVD `heatp_lowmid` — Pixel 4a profile, Android 11 (API 30, google_apis x86_64),
  **2 CPU cores**, **2 GB RAM**, `hw.gpu.mode=host`. Deliberately low-mid, not the S23. Same
  emulator class the 2026-09-02 TimeGo pass and HeatP's 1.6 pass used.
- **Build:** R8 release (`assembleRelease`), debug-keystore-signed, `base-release.apk`
  (2,009,723 bytes), commit `646773b` (`master` == `origin/master`).
- **Seed data:** the app's own 820-exercise catalogue (seeded on first launch), then
  `docs/perf-seed-2026-09-03.sql` injected via direct SQLite:
  **1,500 closed sessions / 30,003 set_logs** spread over ~5 years (deterministic, seed=20260903).
  Matches the 2026-09-02 "five-year reference workload" (1,500 / 30,000 / 820).
- **Method:** `am start -W -S` ×6 for cold start (first dropped as warm-up); per screen —
  `dumpsys gfxinfo <pkg> reset`, 14–16 up+down swipe cycles, then read `gfxinfo`. `adb unroot`
  before input injection (rooted adbd on this image loses `INJECT_EVENTS`).

## Cold start

`am start -W -S com.lsing.timego/.MainActivity`, runs 2–6: **257, 240, 230, 232, 251 ms**
→ median **240 ms**, TotalTime ≈ WaitTime throughout. Heavy derivations are subscription-gated
and off the startup path, so the 30k-set dataset does not move cold start.

## Scroll (gfxinfo, per screen, R8 release)

| Screen   | Frames | Janky        | 50th | 90th | 95th | 99th | Missed Vsync |
|----------|--------|--------------|------|------|------|------|--------------|
| Log      | 523    | 154 (29.4%)  | 5 ms | 20 ms| 22 ms| 61 ms| 4 |
| Progress | 487    | 52 (10.7%)   | 5 ms | 17 ms| 19 ms| 21 ms| 1 |
| Routines | 725    | 0 (0.0%)     | 5 ms | 5 ms | 5 ms | 7 ms | 0 |

**Interpretation.** The 50th percentile is 5 ms on every screen — the app/UI thread keeps up.
The elevated jank flag on Log/Progress is the same 2-core-guest artefact HeatP's 1.6 baseline
documented: the GPU histogram concentrates at 13–16 ms (emulated tile flush), and the jank flag
on this image swings 0–98 % with host-CPU load between otherwise identical runs. No missed-vsync
storm, no frames-over-150ms cluster beyond one-off outliers. Routines (the plain `LazyColumn`
list) is the honest reading: 0 % jank, 7 ms 99th.

## Read

Runtime performance is already strong on a throttled 2-core/2 GB device with a five-year dataset,
consistent with the 2026-09-02 measured pass. The remaining optimisation value is expected in
**code quality** and **APK/dependency size**, not frame timing — the same conclusion HeatP's
Update 1.6 audit reached. Any performance change this pass makes must be backed by a JVM
derivation probe, not the emulator jank flag.
