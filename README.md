# TimeGo

Local-first Android gym tracker. Log strength, calisthenics, holds and cardio, get deterministic overload and
rest guidance, and review progress, all on-device.

## Features

- **Log**: freeform or routine sessions, a Today's Workout card that picks the best-recovered routine, an exercise
  library with search and custom exercises, favorites, numbered set logging with target RPE, a rest timer (adaptive
  or static) with vibration and notification, per-exercise dumbbell entry (each or combined), and timed holds that
  can run once per side.
- **Programs & Routines**: your own program, custom routines and ten preinstalled programs (PPL, Upper/Lower, Arnold,
  PHUL, PHAT, GZCLP and more). Every routine is editable, and preinstalled ones can be reset to default.
- **Progress**: consistency heatmap, muscle distribution body map, strength curves, personal records, body weight
  and BMI trend.
- **Recommendations**: rule-based progressive overload with RPE-gated escalation, plateau and deload detection,
  hold-duration progression, muscle-recovery routine rotation. Deterministic and inspectable, not a black box.
- **Appearance**: dark "Engine-Room" theme and a light "Daylight" theme, selectable in Settings.

## Stack

| | |
|---|---|
| Language/UI | Kotlin, Jetpack Compose, Material 3 |
| Storage | Room (local SQLite, schema 17), Preferences DataStore |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 37 |

## Privacy

- No account, no backend, no cloud sync in this build.
- No `INTERNET` permission.
- Android auto-backup and device transfer are explicitly disabled for app data.
- All workout data stays on-device. Backups are explicit file export and restore from Settings.

## Build

```
git clone https://github.com/PheNOm004/workout-tracker.git
cd workout-tracker
./gradlew installDebug
```

The debug build installs as `com.lsing.timego.debug`, side by side with a release install. Requires Android Studio
or a JDK per the Gradle wrapper. No backend setup or API keys needed.

## Repository layout

| Path | Contents |
|---|---|
| `app/` | The Android app |
| `catalogue/` | Bundled exercise catalogue (824 entries) and its schema |
| `scripts/` | Catalogue generation and validation, Play release helpers |
| `docs/release/` | Play release checklists, privacy and data-safety drafts |
| `ml-prototype/` | Offline research for the adaptive-coach model (Python, laptop only, not shipped) |
| `functions/`, `firebase/`, `hosting/`, `public-site/` | Optional backend foundation and the public landing and privacy pages |

Specs, implementation plans, audits and design history are kept outside the repository, in the author's project vault.

## Status

Personal single-user project, actively developed. An on-device adaptive-coach capability model is in progress behind
evidence gates; the research lives in `ml-prototype/`.
