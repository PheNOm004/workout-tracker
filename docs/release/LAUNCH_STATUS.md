# TimeGo launch status

## Verified on the development branch

- Application ID is `com.lsing.timego`; target SDK is API 37.
- Guest workout logging remains available without an account.
- Email/password account creation, sign-in, reset request, verification refresh, reauthentication,
  and deletion flows are implemented behind Firebase configuration.
- Cloud backup is explicit, off by default, owner-scoped, bounded, and retryable.
- Weekly and monthly reports are independent, off by default, timezone-aware, consent-gated, and
  locally previewable; the backend rechecks cloud consent before sending.
- Training-profile onboarding stores canonical metric values, supports metric/imperial entry, and
  feeds equipment-aware recommendations without uploading private notes.
- The catalogue contains 820 validated entries and exactly 200 complete common guides.
- Android and Functions quality gates are available locally and in GitHub Actions.

## External release inputs still required

1. Create the Firebase project and supply its uncommitted `app/google-services.json`.
2. Configure Email/Password Auth, Firestore rules, Functions secrets, sender-domain/DNS, and an
   email-provider sandbox test.
3. Replace all policy, support, screenshot, feature-graphic, and deletion-page placeholders, then
   deploy Hosting only after reviewing them.
4. Create the Play Console app, enroll Play App Signing, create the private upload keystore, and
   build the signed AAB with `scripts/build-play-aab.ps1`.
5. Complete internal/closed testing, declarations, data-safety answers, account-deletion review,
   and staged rollout in Play Console.

No Firebase project, production secret, signing key, Play Console mutation, or public deployment is
contained in this repository or performed by local verification.
