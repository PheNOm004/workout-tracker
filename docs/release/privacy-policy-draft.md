# TimeGo Privacy Policy — Publication Draft

**Application:** TimeGo (`com.lsing.timego`)
**Effective date:** [PUBLICATION INPUT: EFFECTIVE DATE]
**Developer:** [PUBLICATION INPUT: PUBLIC DEVELOPER NAME]
**Privacy contact:** [PUBLICATION INPUT: SUPPORT EMAIL]

This draft must be reconciled with the exact release artifact and deployed at a stable public HTTPS
URL before publication. It is not a statement that unfinished cloud features are currently live.

## Local use

TimeGo stores exercises, routines, workout sessions, set logs, body measurements, training
preferences, recommendation state, and optional private movement-limitation notes on the user's
device. Core workout logging works without an account. Android backup is disabled; users can choose
to export or restore a local database backup.

## Optional account and cloud backup

If enabled in a release, Firebase Authentication processes the email address, verification state,
authentication identifiers, and security metadata needed to operate an account. Passwords are
handled by Firebase Authentication and are not stored by TimeGo.

Creating an account does not itself enable workout upload. After separate cloud-backup consent,
TimeGo may synchronize the training profile and selected workout, routine, and body-metric records
to account-isolated Firebase storage. Private free-text limitation notes are excluded. Cloud backup
can be disabled without preventing local workout logging.

## Optional email reports

Weekly and monthly reports are separate opt-in choices. Report consent includes cadence, destination
email, timezone, consent-policy version, and timestamp. To create a report, the service processes
the synchronized workout information needed for that reporting period. Delivery uses
[PUBLICATION INPUT: EMAIL DELIVERY PROVIDER]. Report delivery records retain cadence, period,
delivery status, and provider message identity; they do not retain an additional full report body.
Every report includes a manage or unsubscribe link.

## Exercise catalogue and recommendations

The exercise catalogue ships with the app and does not require GitHub at runtime. Recommendations
are generated from local workout records and explicit profile preferences. TimeGo does not sell
personal data or use it for advertising. No analytics or advertising SDK is included by default.

## Security and retention

Network features use encrypted HTTPS transport. Cloud access is restricted to the authenticated
account and authorized backend services. Local information remains until the user deletes it,
clears app storage, uninstalls the app, or replaces it through an explicit restore. Cloud account
information remains while the account or opted-in service is active and is deleted through the
account deletion process, subject only to retention that is specifically disclosed here before
publication: [PUBLICATION INPUT: CONFIRMED RETENTION EXCEPTIONS OR "NONE"].

## User choices and deletion

Users can edit or reset training preferences, disable cloud backup, unsubscribe from weekly or
monthly reports independently, export local data, and delete an account and associated cloud data
from Account settings. A public account deletion route is available at
[PUBLICATION INPUT: ACCOUNT DELETION HTTPS URL]. Deleting the cloud account does not silently erase
local workout history; the user receives a separate local-deletion choice.

## Children, medical use, and changes

TimeGo is a general fitness log, not a medical device and not a source of diagnosis or treatment.
The Play target audience and any minimum-age restriction must match the final onboarding/account
configuration: [PUBLICATION INPUT: CONFIRMED TARGET AGE]. Material policy changes will update the
effective date and, where required, request renewed consent.
