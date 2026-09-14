# TimeGo Play Release Runbook

TimeGo's permanent Android application ID is `com.lsing.timego`. A Play candidate is an Android App
Bundle signed with a private upload key. Google Play App Signing should hold the separate app-signing
key used for distributed APKs.

## One-time user-controlled setup

1. Create the Play Console application with package `com.lsing.timego`.
2. Enrol it in Play App Signing and securely archive the recovery information.
3. Generate a private upload key whose validity extends beyond 22 October 2033.
4. Keep the keystore outside this repository and back it up in two protected locations.
5. Set these values only in the current shell or a protected CI secret store:

```powershell
$env:TIMEGO_UPLOAD_STORE_FILE = '<absolute path to upload keystore>'
$env:TIMEGO_UPLOAD_STORE_PASSWORD = '<upload store password>'
$env:TIMEGO_UPLOAD_KEY_ALIAS = '<upload key alias>'
$env:TIMEGO_UPLOAD_KEY_PASSWORD = '<upload key password>'
```

Do not place real values in this document, Gradle properties, source control, screenshots, build
logs, or support messages.

## Candidate build

Increment `versionCode` for every uploaded artifact. Update `versionName` when the user-facing
release label changes. From a clean approved commit:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

Or use the repository wrapper, which refuses to run until all four signing variables are present
and runs the repository preflight first:

```powershell
.\scripts\build-play-aab.ps1
```

Add `-Firebase` only after the production `app/google-services.json` has been supplied locally.

`assembleRelease` intentionally remains available without signing values for local R8 verification.
`bundleRelease` produces an uploadable signed bundle only when all four environment values are set.
Before upload, confirm the bundle certificate matches the Play Console upload certificate. After
Play processes the bundle, test the Play-distributed artifact because its app-signing certificate is
different from the upload certificate.

Run the repository-owned gate from a clean `master` or approved public-foundation branch:

```powershell
.\scripts\verify-play-release.ps1
```

After producing a signed AAB, supply its path to request signature and bundletool validation:

```powershell
.\scripts\verify-play-release.ps1 -BundlePath '.\app\build\outputs\bundle\release\app-release.aab'
```

The bundle check reports an incomplete gate rather than success if `jarsigner` or `bundletool` is
not available.

## Play progression

1. Upload to Internal testing and inspect App Bundle Explorer output.
2. Install from the Play opt-in link and verify offline logging, database migration, backup export,
   onboarding, account/report consent, deletion, and sign-in callbacks.
3. Resolve automated pre-launch, policy, accessibility, and device compatibility findings.
4. Complete the required closed test and production-access process for the developer account.
5. Use a staged production rollout and monitor crashes, ANRs, authentication, synchronization, and
   report delivery before increasing availability.
