# TimeGo Data Safety Matrix

This matrix maps the planned `com.lsing.timego` public release to Play Console answers. Re-audit the
merged manifest, dependencies, Firebase configuration, backend, and email provider immediately
before submission; Play answers must describe shipped behavior, not this plan.

| Data or capability | Local by default | Leaves device only when | Purpose | Shared processor | Deletion |
|---|---:|---|---|---|---|
| Workout sessions and sets | Yes | Verified user enables cloud backup | App functionality, backup, reports | Firebase | Account deletion or local deletion |
| Routines and custom exercises | Yes | Verified user enables cloud backup | Backup/restore | Firebase | Account deletion or local deletion |
| Height, weight, waist | Yes | Cloud backup is enabled and inclusion is confirmed | Progress and optional reports | Firebase | Account deletion or local deletion |
| Training goal, experience, equipment, schedule | Yes | Cloud backup is enabled | Recommendations and backup | Firebase | Account deletion or profile reset |
| Private limitation note | Yes | Never | Local reminder only | None | Profile reset or local deletion |
| Email address and auth metadata | No account required | User creates an account | Authentication and security | Firebase Authentication | Account deletion |
| Weekly/monthly report preferences | No | User separately enables a cadence | Email report delivery | Firebase and email provider | Unsubscribe or account deletion |
| Report content | Local preview | Enabled report is generated | User-requested communication | Firebase function and email provider | Period processing and provider policy |
| Delivery status/message ID | No | Report delivery is attempted | Reliability and duplicate prevention | Firebase and email provider | Account deletion/defined retention |

## Declarations to reconcile

- **Data Safety:** distinguish collection from on-device processing and declare every cloud/email
  processor consistently with its final contract.
- **Health Apps:** declare fitness/workout tracking even without Health Connect.
- **Data deletion:** answer yes to account creation only when both in-app deletion and the public
  deletion URL are working.
- **Security practices:** claim encryption in transit and deletion only after end-to-end evidence.
- **Optional data:** do not describe cloud backup, weekly reports, or monthly reports as mandatory.
- **No sale/advertising:** verify the final dependency graph before making either claim.

Publication inputs: privacy-policy URL, deletion URL, support email, public developer identity,
Firebase region/retention, email provider/subprocessors, target age, and confirmed retention period.
