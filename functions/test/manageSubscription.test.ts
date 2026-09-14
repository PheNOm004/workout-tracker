import { describe, expect, it, vi } from "vitest";
import { createManageToken, unsubscribeWithToken, verifyManageToken } from "../src/reports/manageSubscription.js";

const secret = "a-secure-test-secret-with-more-than-32-characters";

describe("report manage tokens", () => {
  it("round-trips valid single-purpose claims", () => {
    const token = createManageToken({ uid: "u1", cadence: "weekly", expiresAtEpochSeconds: 200, nonce: "n1" }, secret);
    expect(verifyManageToken(token, secret, 100)).toEqual({ uid: "u1", cadence: "weekly", expiresAtEpochSeconds: 200, nonce: "n1" });
  });
  it("rejects expired and tampered tokens", () => {
    const token = createManageToken({ uid: "u1", cadence: "all", expiresAtEpochSeconds: 99, nonce: "n1" }, secret);
    expect(() => verifyManageToken(token, secret, 100)).toThrow("expired");
    expect(() => verifyManageToken(`${token}x`, secret, 1)).toThrow("invalid");
  });
  it("supports cadence-only unsubscribe and rejects replay", async () => {
    const token = createManageToken({ uid: "u1", cadence: "monthly", expiresAtEpochSeconds: 200, nonce: "n1" }, secret);
    const update = vi.fn();
    expect(await unsubscribeWithToken({ token, secret, nowEpochSeconds: 100, consumeNonce: async () => true, update })).toBe("monthly");
    expect(update).toHaveBeenCalledWith("u1", "monthly");
    await expect(unsubscribeWithToken({ token, secret, nowEpochSeconds: 100, consumeNonce: async () => false, update })).rejects.toThrow("already used");
  });
});
