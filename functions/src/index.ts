import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { HttpsError, onCall, onRequest } from "firebase-functions/v2/https";
import { deleteOwnedAccount } from "./deleteAccount.js";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineSecret } from "firebase-functions/params";
import { FirestoreDeliveryLedger } from "./reports/firestoreDelivery.js";
import { HttpEmailProvider } from "./reports/HttpEmailProvider.js";
import { scheduleReports } from "./reports/scheduleReports.js";
import { verifyManageToken } from "./reports/manageSubscription.js";

initializeApp();

const emailEndpoint = defineSecret("REPORT_EMAIL_ENDPOINT");
const emailApiKey = defineSecret("REPORT_EMAIL_API_KEY");
const emailFrom = defineSecret("REPORT_EMAIL_FROM");
const manageBaseUrl = defineSecret("REPORT_MANAGE_BASE_URL");
const manageSecret = defineSecret("REPORT_MANAGE_TOKEN_SECRET");

const scheduledSecrets = [emailEndpoint, emailApiKey, emailFrom, manageBaseUrl, manageSecret];
function scheduledReport(cadence: "weekly" | "monthly") {
  return onSchedule({ schedule: "every hour", secrets: scheduledSecrets, region: "australia-southeast1" }, async () => {
    const firestore = getFirestore();
    await scheduleReports({
      cadence, now: new Date(), firestore,
      provider: new HttpEmailProvider(emailEndpoint.value(), emailApiKey.value(), emailFrom.value()),
      ledger: new FirestoreDeliveryLedger(firestore), manageBaseUrl: manageBaseUrl.value(), manageSecret: manageSecret.value(),
    });
  });
}

export const scheduleWeeklyReports = scheduledReport("weekly");
export const scheduleMonthlyReports = scheduledReport("monthly");

export const manageReportSubscription = onRequest({ secrets: [manageSecret], region: "australia-southeast1" }, async (request, response) => {
  try {
    const token = typeof request.query.token === "string" ? request.query.token : "";
    const claims = verifyManageToken(token, manageSecret.value(), Math.floor(Date.now() / 1000));
    const firestore = getFirestore();
    const nonce = firestore.collection("reportManageNonces").doc(claims.nonce);
    const user = firestore.collection("users").doc(claims.uid);
    await firestore.runTransaction(async (transaction) => {
      if ((await transaction.get(nonce)).exists) throw new Error("token already used");
      transaction.create(nonce, { expiresAtEpochSeconds: claims.expiresAtEpochSeconds });
      const changes = claims.cadence === "all" ? { weeklyEnabled: false, monthlyEnabled: false } : { [`${claims.cadence}Enabled`]: false };
      transaction.set(user, changes, { merge: true });
    });
    response.status(200).type("html").send("<!doctype html><html lang='en'><meta name='viewport' content='width=device-width'><title>TimeGo reports</title><main><h1>Report preference updated</h1><p>You have been unsubscribed. Your workout data was not displayed or changed.</p></main></html>");
  } catch {
    response.status(400).type("html").send("<!doctype html><html lang='en'><meta name='viewport' content='width=device-width'><title>TimeGo reports</title><main><h1>Link unavailable</h1><p>This manage link is invalid, expired, or has already been used. You can still change report preferences in TimeGo.</p></main></html>");
  }
});

export const deleteAccount = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in before requesting deletion.");
  const authTime = Number(request.auth?.token.auth_time ?? 0) * 1000;
  if (!authTime || Date.now() - authTime > 5 * 60 * 1000) throw new HttpsError("failed-precondition", "Recent reauthentication required.");
  const firestore = getFirestore();
  const user = firestore.collection("users").doc(uid);
  await deleteOwnedAccount(uid, {
    disableReports: async () => { await user.set({ reportsDisabledAt: Date.now(), weeklyEnabled: false, monthlyEnabled: false }, { merge: true }); },
    deleteOwnedData: async () => { await firestore.recursiveDelete(user); },
    deleteAuthentication: async (id) => {
      try { await getAuth().deleteUser(id); }
      catch (error) { if ((error as { code?: string }).code !== "auth/user-not-found") throw error; }
    },
  });
  return { status: "deleted" };
});
