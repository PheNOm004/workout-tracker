# Report delivery runbook

## Deployment inputs

- Firebase project with Authentication, Firestore, Functions and Scheduler enabled.
- A transactional email provider endpoint, verified sending domain, SPF, DKIM and DMARC.
- Managed secrets for the provider credential and a separate 32+ character manage-token signing key.
- Public HTTPS manage/unsubscribe base URL. Never place provider credentials in the Android app, Git, logs, or Firestore.

## Delivery contract

Weekly and monthly consent are independent and off by default. Scheduling uses each subscription's IANA timezone and an idempotency key of user, cadence and period. The sender must acquire the ledger record transactionally before contacting the provider and persist only the provider message ID and minimal status metadata. Report content and private limitation notes must not enter delivery logs.

All emails include matching plain text and escaped HTML, a signed single-use manage link, and privacy context. Empty periods send only when the user selected that behavior. Provider retryable failures release the ledger for bounded retry; permanent suppression events disable the affected cadence.

## Operations

Monitor due subscriptions, acquired/sent/failed ledger counts, retry age and provider suppression events without logging workout payloads. An incident switch must disable scheduled fan-out while leaving local previews and workout logging operational. Account deletion disables subscriptions before deleting user data and Authentication last.

Production release remains blocked until emulator rules tests, provider sandbox delivery, DNS verification, public manage-page hosting, secret provisioning and an unsubscribe end-to-end test have all passed.
