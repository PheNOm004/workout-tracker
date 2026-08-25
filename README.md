# TimeGo

Local-first Android gym tracker. Log strength, calisthenics, holds, and cardio, get deterministic overload/deload guidance, and review progress, all fully offline.

## Features

- **Log** — freeform or routine-based sessions, exercise library with search and custom exercises, favorites, session day-type labels, cropped muscle-heatmap on the landing page.
- **Progress** — all-time volume heatmap, muscle balance (radar/bars), strength curves, personal records, body weight/BMI trend.
- **Recommendations** — rule-based progressive overload with RPE-gated escalation, plateau/deload detection, hold-duration progression, muscle-balance nudges. All deterministic and inspectable, not a black-box model.

## Stack

| | |
|---|---|
| Language/UI | Kotlin, Jetpack Compose, Material 3 |
| Storage | Room (local SQLite), Preferences DataStore |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 36 / 37 |

## Privacy

- No account, no backend, no cloud sync.
- No `INTERNET` permission.
- Android auto-backup and device transfer are explicitly disabled for app data.
- All workout data stays on-device.

## Build

```
git clone https://github.com/PheNOm004/workout-tracker.git
cd workout-tracker
./gradlew installDebug
```

Requires Android Studio / JDK per the Gradle wrapper. No backend setup or API keys needed.

## Status

Personal single-user project, actively developed. An on-device adaptive-coach capability model is in progress behind evidence gates; see `docs/` for design notes.
