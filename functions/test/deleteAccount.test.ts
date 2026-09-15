import { describe, expect, it, vi } from "vitest";
import { deleteOwnedAccount } from "../src/deleteAccount.js";

describe("deleteOwnedAccount", () => {
  it("disables delivery, deletes owned data, then deletes authentication", async () => {
    const order: string[] = [];
    await deleteOwnedAccount("user-1", {
      disableReports: async (uid) => { expect(uid).toBe("user-1"); order.push("reports"); },
      deleteOwnedData: async (uid) => { expect(uid).toBe("user-1"); order.push("data"); },
      deleteAuthentication: async (uid) => { expect(uid).toBe("user-1"); order.push("auth"); },
    });
    expect(order).toEqual(["reports", "data", "auth"]);
  });

  it("never deletes authentication if owned-data deletion fails", async () => {
    const deleteAuthentication = vi.fn();
    await expect(deleteOwnedAccount("user-1", {
      disableReports: async () => undefined,
      deleteOwnedData: async () => { throw new Error("retry"); },
      deleteAuthentication,
    })).rejects.toThrow("retry");
    expect(deleteAuthentication).not.toHaveBeenCalled();
  });

  it("requires an authenticated uid", async () => {
    await expect(deleteOwnedAccount("", { disableReports: vi.fn(), deleteOwnedData: vi.fn(), deleteAuthentication: vi.fn() })).rejects.toThrow("authenticated uid required");
  });
});
