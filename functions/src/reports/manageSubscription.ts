import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import type { Cadence } from "./reportPeriods.js";

export interface ManageClaims { uid: string; cadence: Cadence | "all"; expiresAtEpochSeconds: number; nonce: string }

export function createManageToken(claims: Omit<ManageClaims, "nonce"> & { nonce?: string }, secret: string): string {
  if (secret.length < 32) throw new Error("manage-token secret must be at least 32 characters");
  const payload = Buffer.from(JSON.stringify({ ...claims, nonce: claims.nonce ?? randomBytes(16).toString("hex") })).toString("base64url");
  const signature = createHmac("sha256", secret).update(payload).digest("base64url");
  return `${payload}.${signature}`;
}

export function verifyManageToken(token: string, secret: string, nowEpochSeconds: number): ManageClaims {
  const [payload, signature, extra] = token.split(".");
  if (!payload || !signature || extra) throw new Error("invalid token");
  const expected = createHmac("sha256", secret).update(payload).digest();
  const actual = Buffer.from(signature, "base64url");
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) throw new Error("invalid token");
  const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as ManageClaims;
  if (!claims.uid || !claims.nonce || !["weekly", "monthly", "all"].includes(claims.cadence)) throw new Error("invalid token");
  if (claims.expiresAtEpochSeconds < nowEpochSeconds) throw new Error("expired token");
  return claims;
}

export async function unsubscribeWithToken(args: {
  token: string; secret: string; nowEpochSeconds: number;
  consumeNonce: (nonce: string, expiresAtEpochSeconds: number) => Promise<boolean>;
  update: (uid: string, cadence: ManageClaims["cadence"]) => Promise<void>;
}): Promise<ManageClaims["cadence"]> {
  const claims = verifyManageToken(args.token, args.secret, args.nowEpochSeconds);
  if (!await args.consumeNonce(claims.nonce, claims.expiresAtEpochSeconds)) throw new Error("token already used");
  await args.update(claims.uid, claims.cadence);
  return claims.cadence;
}
