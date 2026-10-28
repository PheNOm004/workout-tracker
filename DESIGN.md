# TimeGo design system: Plate Colours

The visual world is the barbell plate: calibrated plate colours on warm rubber-black (dark) and chalk (light). Light and dark are designed separately; neither is an inversion of the other. Source of truth: `ui/theme/Color.kt`, `Type.kt`, `Shapes.kt`, `Motion.kt`.

## Colour

- **Red is the action colour.** Only tappable things are red (buttons, the selected nav pill's container). Labels, eyebrows and captions use `onSurfaceVariant`.
- **Plate colours carry meaning**, never decoration: chest red, back blue, shoulders yellow, legs green, arms white, core and cardio steel (`regionColor`, `RegionChip`). Blue is informational (coach targets, strength charts). Green is progress and consistency. Yellow is plateau, records and the "recommended" pill.
- **Neutrals are warm-tinted** (OKLCH hue 60 to 85, chroma 0.005 to 0.014), never flat grey.
- **Light-mode yellow is split**: `yellowFill` (bright, only behind dark ink text) and `yellow` (darker amber for dots, bars and lines, >= 3:1).
- All text pairs are >= 4.5:1 and graphics >= 3:1 at the default palette. Tokens are `TimeGoPalette` (via `Plate*`, `CardFill`, `RaisedFill`, `Hairline`) plus the Material roles. No hard-coded colours outside `Color.kt`.

## Shape and depth

- Radii: 8 / 12 / 16 / 24 / 28 dp (`TimeGoShapes`). Cards default to 24 dp.
- Depth is a tonal step plus a hairline edge (`SurfaceCard`, `hero` = one level up). No shadows, gradients, bezels or rivets.

## Glass (restricted)

Frosted translucency is used in exactly three places: the floating bottom dock, the rest-timer bar, and modal sheets/dialogs. Cards, set entry, charts and buttons stay solid so figures stay legible in a bright gym.

- Dock and rest bar: `Modifier.frosted(shape)` over a Haze source (`LocalHazeState`). The rest bar uses its own source (the session list) because Haze cannot blur a surface from a source that contains it.
- Sheets and dialogs (`TimeGoBottomSheet`, `TimeGoDialog`): about 90% opaque raised surface over a window-level blur of the app (`BlurBehindWindow`, Android 12+). Below Android 12 only the tint and scrim show.
- Scrolling screens pad their last item by `LocalDockHeight` so content passes under the dock but can always scroll clear of it.

## Type

Manrope for text and headings (headings are ExtraBold with slight negative tracking). JetBrains Mono for every logged number (`LedgerFigure*`) so sets align as a column.

## Motion

Things that move or resize use springs (`TimeGoMotion`: `expandEnter/Exit`, `contentEnter`, `navigationInOffset`, `rotate`, `settle`) so they keep momentum when interrupted. Opacity uses short asymmetric tweens (decelerate in 220 ms, accelerate out 120 ms). Colour never springs (`colorFade`). Press feedback is `tactilePress`, animated in the draw phase.
