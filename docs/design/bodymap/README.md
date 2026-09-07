# TimeGo body map

## Approved design

The user approved the lighter reference-based physique on 2026-09-08. Keep its proportions,
featureless head, two-tone source palette, and small anatomical components. The original
176-shape art is retained in `original-backup`; the integrated drawing has 202 paths.

`bodymap.svg` is the editable source of truth. It has no embedded raster image, fonts, filters,
external references, or gradients. The two source fills are `#20262C` and `#C1C7CC`. App colors
remain theme-aware, with the existing animated heat scale supplying workout intensity.

Changes after silhouette approval:

- Split shoulder caps so front, side, and rear delts have independent coverage.
- Add medial-thigh adductor panels, rather than relabeling a quad or an entire thigh.
- Add restrained chest, arm, scapular, lat, glute, thigh, and calf subdivisions within the
  existing silhouette. Components within one muscle group retain the same intensity.
- Fill broad blank head, wrist/hand, ankle/foot, and central pelvic spaces with neutral contour
  and tendon shapes. Neutral means visually drawn but never driven by workout intensity.

## App integration

Run from the repository root:

```powershell
python docs/design/bodymap/generate.py
```

This standard-library-only generator writes `MuscleBodyArt.kt`. Each SVG path declares
`data-muscle`, `data-outline`, and exact `data-bounds`; each view declares its app viewport as
left/top/right/bottom (`data-viewbox`, distinct from SVG's x/y/width/height). If changing geometry,
update bounds too. Front/back viewports have identical dimensions and vertical alignment.

The app uses Compose's existing SVG path parser, retaining curves and closed subpaths. It
does not load the SVG file at runtime and introduces no runtime dependencies. Crop bounds are
precomputed instead of repeatedly parsing geometry to discover them. Full diagrams, Log crops,
recommendation crops, and the loading skeleton share the generated geometry.

Long-press regions follow paint order: later neutral details or another group's shape occlude
earlier hit targets. The full diagram's zero-volume state, animation, heat colors, legend,
set-summary content, and popup behavior are retained. Workout calculations and storage are untouched.

## Anatomical assignments

All 18 anatomical groups have bilateral drawable coverage. FULL_BODY remains the existing
catch-all expansion/fallback, not a new anatomical zone. Shoulder heads stay independent.
Ten serratus rib-slip components are explicitly labeled `data-anatomy="SERRATUS_ANTERIOR"`
and have `data-muscle="NEUTRAL"`: serratus is not a tracked exercise group, so these shapes
remain visible but do not borrow oblique intensity or produce an oblique readout. Actual
external-oblique regions remain assigned to OBLIQUES.
Neck, patellae, sartorius/tendon details, and anterior shin structures with no matching tracked
group remain neutral; calf bellies are assigned to CALVES. This is a stylized training diagram,
not a medical atlas. References consulted for placement:

- [OpenStax: pectoral girdle and upper limbs](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-5-muscles-of-the-pectoral-girdle-and-upper-limbs)
- [OpenStax: pelvic girdle and lower limbs](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-6-appendicular-muscles-of-the-pelvic-girdle-and-lower-limbs)

## Verification — 2026-09-08

- JVM tests, lintDebug, assembleDebug, assembleRelease, and assembleDebugAndroidTest passed.
- Five geometry/coverage unit tests protect complete bilateral coverage, finite in-viewport
  crop bounds, source lightness, and neutral anatomical extremities.
- Five targeted Android tests passed on `heatp_lowmid`, serial `emulator-5556`: every group's
  visible hit region, non-overlapping targets, neutral occlusion, real long-press/release
  readout, and dark/light rendering at untrained, selective, and FULL_BODY intensities.
- Inspected emulator captures with chest/triceps and adductor-only crops. Captures are in
  `emulator-review/`. These are isolated component scenarios, not the user's workout history.
- No operation was performed on the primary phone. Phone visual acceptance remains pending.

## Reverting

See [original-backup/README.md](original-backup/README.md). The original renderer and art were
saved directly from the pre-change Git version and checked against the manifest's SHA-256 hashes.
The old path parser is retained in the app so the original renderer remains restorable.

### Serratus correction — 2026-09-08

Removed oblique assignment from ten serratus rib-slip components without altering their geometry.
Anatomical identity remains explicit in the SVG; these untracked shapes are rendered neutrally.
Added a regression check for their assignment and Android neutral-hit probes. 300 JVM tests
and five targeted emulator tests passed; refreshed render captures reflect this correction.

## Installed update — 2026-09-08

User explicitly requested install and wrapup. Final JVM/lint/debug/release gate passed.
The exact tested debug APK (`498eecfb1daad4f7af00e6b45ea158fdb952a1c234b69f0f5aa3b28175d5f728`) was installed in place on the primary S23.
A complete database/WAL/SHM/settings snapshot and installed APK were saved under
`C:/Users/lsing/Documents/TimeGo-backups/20260908-014910-bodymap`. SQLite integrity passed.
Post-install schema, counts, per-table content digests, settings hashes, and original install
time all matched the backup. Last update time: 2026-09-08 01:49:27. No automated phone launch
or UI inspection; user visual acceptance remains pending. The original-art restore backup is
unchanged.
