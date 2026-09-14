import { describe, expect, it, vi } from "vitest";
import { deliveryKey } from "../src/reports/deliveryLedger.js";
import { isDue, reportPeriodKey } from "../src/reports/reportPeriods.js";
import { renderReportEmail } from "../src/reports/renderReportEmail.js";
import { sendReport } from "../src/reports/sendReport.js";
import { hasReportDeliveryConsent } from "../src/reports/scheduleReports.js";
import { buildServerReport } from "../src/reports/buildServerReport.js";

const report = { title: "Weekly report", period: "2026-09-07 to 2026-09-13", sessions: 3, activeDays: 3, durationMinutes: 120, workingSets: 20, strengthVolumeKg: 4200, holdSeconds: 60, cardioMinutes: 25, cardioDistanceKm: 4.2, suggestion: "Keep <control> & consistency." };

describe("report scheduling", () => {
  it("requires cloud consent before delivery", () => {
    expect(hasReportDeliveryConsent({ email: "a@example.com", timezoneId: "UTC", cloudBackupEnabled: false })).toBe(false);
    expect(hasReportDeliveryConsent({ email: "a@example.com", timezoneId: "UTC" })).toBe(false);
    expect(hasReportDeliveryConsent({ email: "a@example.com", timezoneId: "UTC", cloudBackupEnabled: true })).toBe(true);
  });
  it("uses the subscription timezone across DST", () => {
    expect(isDue("weekly", "Australia/Sydney", new Date("2026-10-04T21:00:00Z"))).toBe(true);
    expect(isDue("weekly", "America/New_York", new Date("2026-10-05T12:00:00Z"))).toBe(true);
  });
  it("handles month and year boundaries", () => expect(reportPeriodKey("monthly", "Australia/Sydney", new Date("2025-12-31T21:00:00Z"))).toBe("2025-12"));
  it("builds deterministic delivery keys", () => expect(deliveryKey("user_1", "weekly", "2026-09-07")).toBe("user_1_weekly_2026-09-07"));
  it("sanitizes malformed synced metrics", () => {
    const result = buildServerReport("weekly", "2026-09-07", [
      { domainType: "session", stableUuid: "s1", date: "2026-09-08", startEpochMillis: 0, endEpochMillis: 60_000 },
      { domainType: "set_log", sessionUuid: "s1", weightKg: Number.NaN, reps: -3, holdSeconds: Number.POSITIVE_INFINITY },
    ]);
    expect(result.strengthVolumeKg).toBe(0);
    expect(result.holdSeconds).toBe(0);
  });
});

describe("email rendering and delivery", () => {
  it("escapes private report content and keeps plain text", () => {
    const rendered = renderReportEmail(report, "https://example.test/manage?t=abc&x=1");
    expect(rendered.html).toContain("&lt;control&gt; &amp; consistency");
    expect(rendered.html).not.toContain("<control>");
    expect(rendered.text).toContain("Working sets: 20");
  });
  it("includes an optional latest body weight", () => {
    const rendered = renderReportEmail({ ...report, latestBodyWeightKg: 81.5 }, "https://example.test/manage");
    expect(rendered.text).toContain("Latest body weight: 81.5 kg");
    expect(rendered.html).toContain("Latest body weight");
  });
  it("suppresses duplicate sends", async () => {
    const provider = { send: vi.fn() };
    const ledger = { acquire: vi.fn().mockResolvedValue(false), delivered: vi.fn(), failed: vi.fn() };
    expect(await sendReport({ key: "k", email: "a@example.com", report, manageUrl: "https://example.test", provider, ledger })).toBe("duplicate");
    expect(provider.send).not.toHaveBeenCalled();
  });
  it("records provider failures for retry", async () => {
    const provider = { send: vi.fn().mockRejectedValue(new Error("temporary")) };
    const ledger = { acquire: vi.fn().mockResolvedValue(true), delivered: vi.fn(), failed: vi.fn() };
    await expect(sendReport({ key: "k", email: "a@example.com", report, manageUrl: "https://example.test", provider, ledger })).rejects.toThrow("temporary");
    expect(ledger.failed).toHaveBeenCalledWith("k", true);
  });
});
