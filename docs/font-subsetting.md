# Bundled fonts — Latin subset

`app/src/main/res/font/manrope_variable.ttf` and `jetbrains_mono_variable.ttf` are
**Latin-subset** builds of the upstream variable fonts, not the full releases. Each font's
`wght` axis is fully preserved; only glyphs outside the Latin range are dropped.

Coverage kept: Basic Latin, Latin-1 Supplement (accented letters), common punctuation, smart
quotes, en/em dashes, arrows, the middle dot (`·`) and bullet (`•`), a few symbols. Every
character the app renders is covered — verified against every string literal in
`app/src/main/java/**/ui/**`. The `★` in the "★ Favorites" filter label was **already** served
by the system-font fallback (neither upstream font ships U+2605), so the subset changes nothing
there. User-typed text outside Latin-1 falls back to the system font, exactly as before.

Sizes at the 2026-09-04 subset:
- Manrope 165,420 → 63,576 B
- JetBrains Mono 300,144 → 120,728 B
- R8 release APK 2,052,446 → 1,927,506 B (−124,940 B / −6.1 %)

To re-subset after replacing a font with a new upstream version:

```
LATIN="U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+2000-206F,U+2074,U+20AC,U+2122,U+2190-2193,U+2212,U+2215,U+2018-201A,U+201C-201E,U+2026,U+2032-2033,U+2605,U+FEFF,U+FFFD"
python -m fontTools.subset FONT.ttf --unicodes="$LATIN" --layout-features='*' \
  --glyph-names --notdef-outline --name-IDs='*' --output-file=FONT.subset.ttf
```

Recipe carried over from `HeatP/docs/font-subsetting.md` (2026-09-02), with `U+2605` added to
the range list for parity with the app's chip label even though neither font currently has it.
