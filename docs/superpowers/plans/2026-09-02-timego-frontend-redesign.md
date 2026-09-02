# Plan: TimeGo Frontend & Design Overhaul

**Date:** 2026-09-02  
**Spec:** `docs/superpowers/specs/2026-09-02-timego-frontend-redesign.md`

---

## Phase Breakdown

### Phase 1: In-Session Active Workout & Sticky Muscle Filters
1. Enhance `LogViewModel` to provide live session set history (`sessionSetsByExercise: Flow<Map<Long, List<SetLog>>>`).
2. Create `ActiveWorkoutSection` composable displaying active exercise cards with completed set chips/tables (*Set 1: 80kg x 8*).
3. Refactor `ExerciseSections` and `LoggingContent` to use a sticky single-row `MuscleFilterRow` (`All`, `Chest`, `Back`, `Shoulders`, `Arms`, `Legs`, `Core`, `Cardio`, `Favorites`), filtering the list to large, direct exercise cards with 1-tap expansion.
4. Add quick weight (+/- 2.5kg) and rep (+/- 1) stepper assists on `StrengthLogRow`.
5. Run unit tests & verify.

### Phase 2: Log Landing "Command Center"
1. Update `LogLandingContent`:
   - Hero Card: Next workout (today's scheduled routine or recommended muscle focus with 1-tap start).
   - Secondary Card: Last session recap (sets, duration, cropped muscle diagram).
   - Routines Carousel: Quick-launch chips for routines & freeform.
   - Remove redundant radar chart.
2. Run unit tests & verify.

### Phase 3: Progress Screen 2-Segment Refactor
1. Introduce 2-way segment state in `ProgressViewModel` (`ProgressSegment.TRAINING`, `ProgressSegment.BODY`).
2. Refactor `ProgressScreen` with smooth animated crossfade:
   - `Training` segment: Heatmap, Timeframe selector + Muscle Anatomy Diagram + Stat tiles, and the **Unified Exercise Performance Card** (combining PR tiles and Strength Sparkline into one card for the selected exercise).
   - `Body` segment: Weight sparkline, BMI card, input form, and history list.
3. Run unit tests & verify.

### Phase 4: Routines & Quiet Settings Sheet
1. Create `SettingsBottomSheet` containing hold delay stepper, training lean preference, export/restore backup actions, and session deletion trigger.
2. In `RoutinesScreen`, add a quiet settings gear icon in the header and clean up the routine templates display.
3. Run unit tests & verify.

### Phase 5: Motion, Animation & Final Integration Verification
1. Add `Modifier.animateItem()` to exercise and set lists.
2. Enhance `TrainingPulse` / set completion confirmation with smooth feedback.
3. Run full JVM test suite (`.\gradlew.bat testDebugUnitTest`) and release APK build (`.\gradlew.bat assembleRelease`).
