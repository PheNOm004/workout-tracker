# TimeGo — Full three-axis optimisation audit (2026-09-03)

Whole-app audit across runtime performance, code quality, and APK/dependency size.
Base: `master` @ `646773b` (== `origin/master`), schema 15, 264 JVM tests green,
lint 0 errors / 23 warnings, unsigned R8 APK 1,993,512 B.

Follows the method of the 2026-08-25 and 2026-09-02 passes and mirrors HeatP's Update 1.6
audit (`HeatP/docs/OPTIMISATION_AUDIT_2026-09-02.md`). Contract: correctness/safety → visible
speed → battery/allocation/simplicity; preserve every feature, calculation, recommendation, and
the visual design; every change backed by a measurement or a concrete defect.

Measurement rig: `docs/perf-baseline-2026-09-03.md` + `docs/perf-seed-2026-09-03.sql`
(1,500 sessions / 30,003 sets / 820 exercises ≈ 5 years). JVM comparative probes on that scale
(desktop JIT — device is several × slower, but the ratios hold).

---

## Axis A — Runtime performance

Emulator scroll (R8, low-mid AVD): 50th percentile 5 ms on every screen; Routines 0 % jank / 7 ms
99th. Log/Progress jank-flag elevation is the documented 2-core-guest GPU artefact, not app-thread
work. Cold start 240 ms median. **No frame-timing finding.** Two derivation-efficiency findings:

| # | Finding | Evidence | Effort | Risk | Recommend |
|---|---------|----------|--------|------|-----------|
| A1 | `LogViewModel.refreshLandingSummary` calls `exerciseUsageFrequency(allSets, exercisesById)` — a full 30k-set pass — on every combine emission, though the enclosing collector computed the identical map into `exerciseUsageCounts` two lines earlier. | probe: redundant pass ≈ 1.1–1.5 ms JVM (≈ 5–15 ms device), every emission. Already inside `Dispatchers.Default`, so wasted work not jank. | XS | Low | **Apply** — thread `usageCounts` into `refreshLandingSummary` (all 3 call sites already have a fresh count available or can compute once). |
| A2 | `ProgressViewModel`'s combine collector runs its whole derivation set — `personalRecords` + `workoutVolumeRatios` (both full-history, unbounded) + `trainingStats` + `trainingStatsByDay` + `muscleDistributionForTimeframe` + `muscleGroupSetSummaryForTimeframe` + `refreshStrengthCurve` + `refreshSelectedHistory` — on `viewModelScope` (`Dispatchers.Main.immediate`). `LogViewModel` wraps its equivalent block in `withContext(Dispatchers.Default)`; Progress does not. | probe: collector body ≈ 9.8 ms JVM ≈ 40–80 ms device, on every logged set while Progress has been visited. | S | Low–Med | **Apply** — wrap the collector body in `withContext(Dispatchers.Default)`; keep `_state.value =` assignments (StateFlow set is thread-safe) or hop back for them. No output change. |
| A3 | `sessions.associate { it.id to it.date }` rebuilt in the Progress collector and again inside `muscleDistributionForTimeframe`, `muscleGroupSetSummaryForTimeframe`, `muscleBalanceForTimeframe` (≈ 4–5 rebuilds of a 1,500-entry map per emission). Also `workoutVolumeRatios` builds its own. | each build sub-ms; total ≈ 2–3 ms JVM. | S | Med (touches shared domain signatures + tests) | **Fold into A2 only if cheap** — add optional `sessionDateById` params defaulting to the current internal build. Otherwise **note**, revisit if a probe regresses. |
| A4 | All three activity-scoped ViewModels `combine(... repository.setLogs ...)` where `setLogs` = `SELECT * FROM set_logs` — the full 30k-row table materialised into each STARTED screen simultaneously (~30k `SetLog` × up to 3). | Bounded by subscription gating; the 2026-09-02 pass deliberately kept this "in-memory screen snapshot" model. | L | High | **Note only** — architectural; out of scope for an optimisation pass. |

---

## Axis B — Code quality

| # | Finding | File(s) | Effort | Risk | Recommend |
|---|---------|---------|--------|------|-----------|
| B1 | `ModifierParameter` lint ×5 — modifier param not first-optional / not named `modifier`. | `HeatmapGrid.kt:161` (`dotModifier`), `MuscleBodyDiagram.kt:158,302`, `TrainingPulse.kt:26`, `WorkoutHistoryDialog.kt:158` | XS | Low | **Apply** — HeatP applied the equivalent (its B3). Private helpers mostly; reorder / rename. |
| B2 | `ObsoleteSdkInt` — `res/mipmap-anydpi-v26/` is redundant at `minSdk 26`. | `res/mipmap-anydpi-v26/` | XS | Low | **Apply** — merge into `mipmap-anydpi/` (HeatP C4). |
| B3 | `UnusedResources` ×7 — every entry in `res/values/colors.xml` (`purple_200/500/700`, `teal_200/700`, `black`, `white`) is unused; it is the unmodified Android Studio template file. No `@color/` / `R.color.` reference anywhere (launcher background is a drawable). | `res/values/colors.xml` | XS | Low | **Apply** — delete the file (HeatP kept its unused colours as "R8 strips them anyway"; here it is the whole template file, worth removing). |
| B4 | Candidate dead domain functions never referenced outside their own file: `synergisticPartnersFor`, `previousMuscleBalanceForTimeframe`, `muscleBalanceForDateRange`, `roundDownToIncrement`, and the HeatP-derived heatmap helpers `calendarMonthWindow` / `calendarYearsSpanned` / `defaultLoadedYears` / `habitHeatmapColorHexes`. | `domain/MuscleBalance.kt`, `domain/MuscleDistribution.kt`, `domain/*` | S | Low–Med | **Verify then apply** — confirm zero prod + test refs per function before removing; keep any a pending backlog piece needs (radar/balance history). |
| B5 | Hidden adaptive-coach package (`data/adaptive/`, `domain/adaptive/`) — no UI caller. `usableShadowCache` / `persistShadowCache` capture a full snapshot + fingerprint per call. | `data/WorkoutRepository.kt`, `data/adaptive/*`, `domain/adaptive/*` | — | — | **Audit only** — findings written up for next session's coach work (see below); do not tune provisional code now. |

### B5 — adaptive-coach notes for next session

- `WorkoutRepository.usableShadowCache` and `persistShadowCache` each call `captureShadowSnapshot()`
  inside their own `withTransaction`, and `rebuildShadowCache` calls `shadowSnapshot()` then feeds
  the pipeline — so a rebuild-then-verify cycle captures the full session/set/exercise snapshot
  **three times**. When the coach is wired, capture once per session-close and pass the snapshot
  through; never invoke any of these from render or per-set code.
- `captureShadowSnapshot` reads `allForShadowSnapshot()` on all three tables (full 30k sets) — fine
  once per session-close, unusable per emission.
- `ShadowCachePipeline.build` filters `mapping.observations` once **per `ShadowBasis`** (`entries.map { basis -> observations.filter { it.basis == basis } }`) — O(bases × observations). Group by basis once instead.
- The shadow tables (`shadow_snapshots`, `shadow_audit`) have no indexes; `shadow_audit` grows
  unbounded. Add a retention cap or prune-on-write when the feature ships.

---

## Axis C — APK / dependencies

Unsigned R8 APK 1,993,512 B; signed 2,009,723 B. `classes.dex` 2.8 MB, `resources.arsc` 140 KB,
`res/font/` 465 KB (**~23 % of the APK**).

| # | Finding | Effort | Risk | Recommend |
|---|---------|--------|------|-----------|
| C1 | AGP 9.3.0 → 9.4.0; Gradle wrapper 9.5.0 → 9.6.0. Lint `AndroidGradlePluginVersion`. | S | Low | **Apply** — isolated commit, full gate (HeatP C5). |
| C2 | Dependency drift (`GradleDependency` ×7): `core-ktx` 1.10.1→1.19.0, `activity-compose` 1.8.0→1.13.0, `navigation-compose` 2.9.8→2.10.0, `datastore-preferences` 1.1.7→1.2.1, `compose-bom` 2026.02.01→2026.08.00, test `androidx.test.ext:junit` 1.1.5→1.3.0, `espresso-core` 3.5.1→3.7.0. | S | Low–Med | **Apply** — group as (a) compose BOM, (b) androidx runtime libs, (c) test libs; gate each. |
| C3 | `NewerVersionAvailable` — `kotlin.plugin.compose` 2.2.10 → 2.4.10. Must track the Kotlin version (2.2.10). | — | — | **Document, do not bump in isolation** (HeatP did the same). |
| C4 | `OldTargetApi` — `targetSdk 36`, `compileSdk 37.1`. HeatP runs `targetSdk 37`. | S | **Med** | **Apply with care** — `targetSdk` 36→37 can change runtime behaviour (predictive back, edge-to-edge, permission prompts). Isolated commit; verify on device before merge. Defer if anything regresses. |
| C5 | `res/font/jetbrains_mono_variable.ttf` 300 KB + `manrope_variable.ttf` 165 KB = 465 KB. Both fully used (4 weights each via the variable axis). A Latin subset via `fonttools` (recipe: `HeatP/docs/font-subsetting.md`) should recover ~250–300 KB. | M | Med | **Apply if a verified subset renders identically** (all string-resource glyphs covered; monospace digits intact); else document. |
| C6 | `material-icons-extended` dependency. | — | — | **Close as moot** — `classes.dex` is 2.8 MB, icon members not exploded in; R8 full mode strips it, same as HeatP (its C2). Keep the dependency. |
| C7 | `proguard-rules.pro` exists (all comments) but is **not** wired — `buildTypes.release` uses only `optimization { enable = true }`, no `proguardFiles(...)`. Long-standing TimeGo open item. | XS | Low | **Apply** — add `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")` so custom keeps are actually applied; confirm R8 output unchanged (file is empty of rules today). |
| C8 | No `room-testing` (`MigrationTestHelper`), `kotlinx-coroutines-test`, or Turbine. Migration tests hand-build schema DBs and use `allowMainThreadQueries`; the data + ViewModel layers have **no** tests. Schemas 3–15 exported. | S | Low | **Apply** — add the three test deps to `libs.versions.toml` + `app/build.gradle.kts`; leave writing new tests as a follow-up task (HeatP's C6/C8-equivalent). |
| C9 | `compileOptions` = `VERSION_11`. AGP 9 requires JDK 17. | XS | Low | **Apply** — bump `sourceCompatibility`/`targetCompatibility` to `VERSION_17` (HeatP C7); fold into the C1 commit. |

---

## Recommended apply-set

- **High-confidence, low-risk:** A1, A2, B1, B2, B3, C1, C7, C8, C9.
- **Verify-then-apply:** B4 (dead-code, per-function ref check), C2 (grouped + gated), C5 (fonts, only if subset renders identically).
- **Apply with device verification before merge:** C4 (`targetSdk` 36→37).
- **Fold-in if cheap, else note:** A3.
- **Document / no code change:** A4, C3, C6. **Audit only:** B5 (adaptive-coach — notes above).

Each applied finding: one commit on `optimisation-2026-09-03`, gated on
`lintDebug testDebugUnitTest assembleDebug assembleRelease` before the next. Full R8 build +
emulator re-measure at the end, `versionCode`/`versionName` bump as its own commit, then merge to
`master` (`--no-ff`) and push after the user accepts the S23 build.

---

## Applied (2026-09-03/04, branch `optimisation-2026-09-03`)

| Finding | Commit | Result |
|---------|--------|--------|
| docs | `f150e94` | audit + baseline + seed |
| A1 | `8393401` | one shared `exerciseUsageFrequency` pass per `LogViewModel` emission (was 2) |
| A2 | `612ca29` | `ProgressViewModel` derivations wrapped in `Dispatchers.Default` (was `Main.immediate`) |
| B1 | `2a37d1f` | `ModifierParameter` lint 5 → 0 |
| B2 | `a4972a2` | `mipmap-anydpi-v26` removed; `ObsoleteSdkInt` cleared |
| B3 | `52d44de` | template `colors.xml` deleted; 7 `UnusedResources` cleared |
| B4 | `9068884` | `YearWindow.kt` + `habitHeatmapColorHexes` + 6 dead tests removed (kept the hex toolkit, `previousMuscleBalanceForTimeframe`) |
| C7+C9 | `db6ceae` | JDK 17 source/target; `proguard-rules.pro` wired into the release R8 config |
| C1 | `5127756` | AGP 9.3.0 → 9.4.0, Gradle 9.5.0 → 9.6.0 |
| C2 (1) | `136d547` | Compose BOM 2026.02.01 → 2026.08.00, core-ktx / activity / navigation / datastore |
| C2 (2) | `efc115e` | test `androidx.test.ext:junit` 1.3.0, `espresso-core` 3.7.0 |
| C4 | `80834e0` | `targetSdk` 36 → 37 (device-verified before merge) |
| C5 | `f36b448` | Latin-subset fonts: Manrope 165→64 KB, JetBrains Mono 300→121 KB |
| version | `b87af96` | `versionCode` 3→4, `versionName` 1.1→1.2 |

**Deferred / not done this pass:**
- **A3** — the ~1500-entry `sessionDateById` map is still rebuilt inside three timeframe functions.
  After A2 that work is off the main thread, and threading an optional param through three shared
  domain signatures + their tests is more churn than the sub-ms/emission gain warrants. Revisit
  only if a probe regresses.
- **C8** — `room-testing` / `kotlinx-coroutines-test` / Turbine not added. No test uses them yet
  (migration tests hand-roll their schema DBs); add alongside the first data/ViewModel test that
  needs them rather than committing unused dependencies.
- **C3** — `kotlin.plugin.compose` 2.4.10 available; left pinned to Kotlin 2.2.10 (must move together).
- **C6** — `material-icons-extended` kept; R8 full-mode strips it (`classes.dex` 2.8 MB, members not present).
- **A4** — full `set_logs` table in memory ×3 ViewModels: architectural, out of scope.
- **B5** — adaptive-coach: audit only; findings recorded above for next session's coach work.

**Result — lint:** 0 errors, 23 → **2** warnings (`AndroidGradlePluginVersion`,
`NewerVersionAvailable` — both toolchain-version notices, intentionally not chased). Matches HeatP's
1.6 end state.

**Result — tests:** 264 → **258** JVM tests (6 removed with the dead code), 0 failures throughout.

**Result — APK:** unsigned R8 1,993,512 → **1,927,506 B** (−66,006 B / −3.3 %); signed
2,009,723 → **1,943,853 B**. Font subset −280 KB of raw TTF, partly offset by the newer androidx
runtime libraries.

**Result — performance:** cold start **~240–285 ms** median vs 240 ms baseline — neutral (the higher
end is a hours-warm emulator). Post-pass scroll on the low-mid AVD with a quiet host, five-year
workload: **Log 1,413 frames / 0.00 % jank / 5 ms 50th / 7 ms 99th**; **Progress 1,240 frames /
0.00 % jank / 5 ms 50th / 8 ms 99th** — both clean, confirming A2 (Progress derivation off the main
thread) holds at scale. The pre-pass baseline's elevated jank flag was host-CPU contention (see
`perf-baseline-2026-09-03.md`); with the host quiet, before and after both read clean. No rendering
or list code changed; A1/A2 move background derivation work only.
