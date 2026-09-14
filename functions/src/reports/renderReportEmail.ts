export interface EmailReport {
  title: string; period: string; sessions: number; activeDays: number; durationMinutes: number;
  workingSets: number; strengthVolumeKg: number; holdSeconds: number; cardioMinutes: number; cardioDistanceKm: number; suggestion: string;
}

const escapeHtml = (value: string) => value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#39;");

export function renderReportEmail(report: EmailReport, manageUrl: string): { subject: string; text: string; html: string } {
  const rows = [
    ["Sessions", report.sessions], ["Active days", report.activeDays], ["Training time", `${report.durationMinutes} min`],
    ["Working sets", report.workingSets], ["Strength volume", `${report.strengthVolumeKg} kg`], ["Holds", `${report.holdSeconds} sec`],
    ["Cardio", `${report.cardioMinutes} min / ${report.cardioDistanceKm} km`],
  ] as const;
  const text = `${report.title}\n${report.period}\n\n${rows.map(([label, value]) => `${label}: ${value}`).join("\n")}\n\n${report.suggestion}\n\nManage or unsubscribe: ${manageUrl}`;
  const htmlRows = rows.map(([label, value]) => `<tr><th align="left">${escapeHtml(label)}</th><td>${escapeHtml(String(value))}</td></tr>`).join("");
  const html = `<main><h1>${escapeHtml(report.title)}</h1><p>${escapeHtml(report.period)}</p><table>${htmlRows}</table><p>${escapeHtml(report.suggestion)}</p><p><a href="${escapeHtml(manageUrl)}">Manage or unsubscribe</a></p><small>This private report was requested in TimeGo. It is not medical advice.</small></main>`;
  return { subject: `${report.title} — ${report.period}`, text, html };
}
