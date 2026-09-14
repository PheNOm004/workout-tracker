import type { Cadence } from "./reportPeriods.js";

export function deliveryKey(uid: string, cadence: Cadence, period: string): string {
  if (!/^[A-Za-z0-9_-]+$/.test(uid)) throw new Error("invalid uid");
  if (!/^\d{4}-\d{2}(-\d{2})?$/.test(period)) throw new Error("invalid period");
  return `${uid}_${cadence}_${period}`;
}

export interface DeliveryLedger {
  acquire(key: string): Promise<boolean>;
  delivered(key: string, providerMessageId: string): Promise<void>;
  failed(key: string, retryable: boolean): Promise<void>;
}
