# TimeGo Firebase setup

Normal development builds keep accounts unavailable and preserve full guest use. To build with the configured Firebase project:

1. Register Android application `com.lsing.timego` in the Firebase Console.
2. Enable Email/Password under Authentication sign-in methods.
3. Download that app's `google-services.json` into `app/`. The file is ignored by Git.
4. Build with `./gradlew bundleRelease -PtimegoFirebase=true` plus the documented `TIMEGO_UPLOAD_*` signing environment variables.

The opt-in property deliberately fails configuration when the JSON file is absent. Authentication does not enable Firestore backup or email reports; those require separate in-app consent and their own deployment gates.

For local auth verification, use the Firebase Emulator Suite and test accounts only. Do not use production workout records in emulator fixtures.

For an authenticated operator, the repository deployment wrapper makes the target explicit and
checks required files before invoking the CLI:

```powershell
.\scripts\deploy-firebase.ps1 -ProjectId '<firebase-project-id>'
```

Production deployment remains an explicit operator action and is not run by Android builds or CI.
