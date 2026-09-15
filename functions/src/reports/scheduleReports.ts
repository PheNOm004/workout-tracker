import type { Firestore } from "firebase-admin/firestore";
import type { Cadence } from "./reportPeriods.js";
import { buildServerReport } from "./buildServerReport.js";
import { deliveryKey } from "./deliveryLedger.js";
import { createManageToken } from "./manageSubscription.js";
import { isDue, reportPeriodKey } from "./reportPeriods.js";
import { sendReport } from "./sendReport.js";
import type { EmailProvider } from "./EmailProvider.js";
import type { DeliveryLedger } from "./deliveryLedger.js";

export function hasReportDeliveryConsent(data: { email?: string; timezoneId?: string; cloudBackupEnabled?: boolean }): boolean {
  return Boolean(data.email && data.timezoneId && data.cloudBackupEnabled === true);
}

export async function scheduleReports(args: { cadence: Cadence; now: Date; firestore: Firestore; provider: EmailProvider; ledger: DeliveryLedger; manageBaseUrl: string; manageSecret: string }): Promise<number> {
  const field = args.cadence === "weekly" ? "weeklyEnabled" : "monthlyEnabled";
  const snapshot = await args.firestore.collection("users").where(field, "==", true).where("emailVerified", "==", true).limit(200).get();
  let sent = 0;
  for (const user of snapshot.docs) {
    const data = user.data() as { email?: string; timezoneId?: string; cloudBackupEnabled?: boolean };
    // Reports are coupled to the user's explicit cloud-consent boundary. The Android client
    // enforces this prerequisite, but the backend must re-check it before any email leaves.
    if (!hasReportDeliveryConsent(data) || !isDue(args.cadence, data.timezoneId!, args.now)) continue;
    const timezoneId = data.timezoneId!;
    const email = data.email!;
    const period = reportPeriodKey(args.cadence, timezoneId, args.now);
    const rows = (await user.ref.collection("data").get()).docs.map((document) => document.data());
    const token = createManageToken({ uid: user.id, cadence: args.cadence, expiresAtEpochSeconds: Math.floor(args.now.getTime() / 1000) + 60 * 60 * 24 * 30 }, args.manageSecret);
    const result = await sendReport({ key: deliveryKey(user.id, args.cadence, period), email, report: buildServerReport(args.cadence, period, rows), manageUrl: `${args.manageBaseUrl}?token=${encodeURIComponent(token)}`, provider: args.provider, ledger: args.ledger });
    if (result === "sent") sent++;
  }
  return sent;
}
