import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { deleteOwnedAccount } from "./deleteAccount.js";

initializeApp();

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
