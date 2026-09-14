import type { EmailReport } from "./renderReportEmail.js";
import type { Cadence } from "./reportPeriods.js";

type CloudRow = { domainType?: string; stableUuid?: string; sessionUuid?: string; date?: string; startEpochMillis?: number; endEpochMillis?: number; weightKg?: number; reps?: number; holdSeconds?: number; durationMinutes?: number; distanceKm?: number };
const nonNegative = (value: number | undefined): number => typeof value === "number" && Number.isFinite(value) ? Math.max(0, value) : 0;

export function periodBounds(cadence: Cadence, periodKey: string): { start: string; end: string } {
  if (cadence === "weekly") {
    const start = new Date(`${periodKey}T00:00:00Z`);
    const end = new Date(start); end.setUTCDate(end.getUTCDate() + 6);
    return { start: periodKey, end: end.toISOString().slice(0, 10) };
  }
  const [year, month] = periodKey.split("-").map(Number);
  const end = new Date(Date.UTC(year, month, 0)).toISOString().slice(0, 10);
  return { start: `${periodKey}-01`, end };
}

export function buildServerReport(cadence: Cadence, periodKey: string, rows: CloudRow[]): EmailReport {
  const bounds = periodBounds(cadence, periodKey);
  const sessions = rows.filter((row) => row.domainType === "session" && row.date && row.date >= bounds.start && row.date <= bounds.end);
  const sessionIds = new Set(sessions.map((row) => row.stableUuid).filter(Boolean));
  const sets = rows.filter((row) => row.domainType === "set_log" && row.sessionUuid && sessionIds.has(row.sessionUuid));
  const latestBodyWeightKg = rows
    .filter((row) => row.domainType === "body_metric" && row.date && row.date >= bounds.start && row.date <= bounds.end && nonNegative(row.weightKg) > 0)
    .sort((left, right) => (left.date! < right.date! ? 1 : -1))[0]?.weightKg;
  return {
    title: cadence === "weekly" ? "Your TimeGo weekly report" : "Your TimeGo monthly report",
    period: `${bounds.start} to ${bounds.end}`,
    sessions: sessions.length,
    activeDays: new Set(sessions.map((row) => row.date)).size,
    durationMinutes: sessions.reduce((sum, row) => sum + nonNegative(((row.endEpochMillis ?? row.startEpochMillis ?? 0) - (row.startEpochMillis ?? 0)) / 60000), 0),
    workingSets: sets.filter((row) => nonNegative(row.reps) > 0).length,
    strengthVolumeKg: sets.reduce((sum, row) => sum + nonNegative(row.weightKg) * nonNegative(row.reps), 0),
    holdSeconds: sets.reduce((sum, row) => sum + nonNegative(row.holdSeconds), 0),
    cardioMinutes: sets.reduce((sum, row) => sum + nonNegative(row.durationMinutes), 0),
    cardioDistanceKm: sets.reduce((sum, row) => sum + nonNegative(row.distanceKm), 0),
    latestBodyWeightKg,
    suggestion: sessions.length ? "Keep the next period consistent and progress only while technique remains controlled." : "No workouts were logged. Start with one manageable session when ready.",
  };
}
