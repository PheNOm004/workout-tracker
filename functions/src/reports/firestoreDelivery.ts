import type { Firestore } from "firebase-admin/firestore";
import type { DeliveryLedger } from "./deliveryLedger.js";

export class FirestoreDeliveryLedger implements DeliveryLedger {
  constructor(private readonly firestore: Firestore) {}
  async acquire(key: string): Promise<boolean> {
    const reference = this.firestore.collection("reportDeliveryLedger").doc(key);
    return this.firestore.runTransaction(async (transaction) => {
      const existing = await transaction.get(reference);
      if (existing.exists && ["sending", "delivered"].includes(existing.get("status"))) return false;
      transaction.set(reference, { status: "sending", acquiredAtEpochMillis: Date.now() }, { merge: true });
      return true;
    });
  }
  async delivered(key: string, providerMessageId: string): Promise<void> {
    await this.firestore.collection("reportDeliveryLedger").doc(key).set({ status: "delivered", providerMessageId, deliveredAtEpochMillis: Date.now() }, { merge: true });
  }
  async failed(key: string, retryable: boolean): Promise<void> {
    await this.firestore.collection("reportDeliveryLedger").doc(key).set({ status: retryable ? "retryable" : "failed", failedAtEpochMillis: Date.now() }, { merge: true });
  }
}
