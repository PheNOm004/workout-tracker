export type Cadence = "weekly" | "monthly";

export function localDateParts(now: Date, timezone: string): { year: number; month: number; day: number; weekday: string; hour: number } {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: timezone, year: "numeric", month: "2-digit", day: "2-digit", weekday: "short", hour: "2-digit", hourCycle: "h23" }).formatToParts(now);
  const value = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? "";
  return { year: Number(value("year")), month: Number(value("month")), day: Number(value("day")), weekday: value("weekday"), hour: Number(value("hour")) };
}

export function isDue(cadence: Cadence, timezone: string, now: Date): boolean {
  const local = localDateParts(now, timezone);
  if (local.hour !== 8) return false;
  return cadence === "weekly" ? local.weekday === "Mon" : local.day === 1;
}

export function reportPeriodKey(cadence: Cadence, timezone: string, now: Date): string {
  const local = localDateParts(now, timezone);
  if (cadence === "monthly") {
    const previousMonth = local.month === 1 ? 12 : local.month - 1;
    const year = local.month === 1 ? local.year - 1 : local.year;
    return `${year}-${String(previousMonth).padStart(2, "0")}`;
  }
  const date = new Date(Date.UTC(local.year, local.month - 1, local.day));
  date.setUTCDate(date.getUTCDate() - 7);
  return date.toISOString().slice(0, 10);
}
