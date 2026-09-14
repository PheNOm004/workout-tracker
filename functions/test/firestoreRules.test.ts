import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const rules = readFileSync(resolve(import.meta.dirname, "../../firebase/firestore.rules"), "utf8");

describe("Firestore security contract", () => {
  it("uses verified owner identity for user documents and descendants", () => {
    expect(rules).toContain("request.auth.uid == uid");
    expect(rules).toContain("request.auth.token.email_verified == true");
    expect(rules).toContain("match /{document=**}");
    expect(rules).toContain("allow read, write: if owns(uid);");
  });

  it("denies every path outside the owner boundary", () => {
    expect(rules).toMatch(/match \/\{document=\*\*\}\s*\{\s*allow read, write: if false;/s);
  });
});
