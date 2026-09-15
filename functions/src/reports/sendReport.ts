import type { DeliveryLedger } from "./deliveryLedger.js";
import type { EmailProvider } from "./EmailProvider.js";
import type { EmailReport } from "./renderReportEmail.js";
import { renderReportEmail } from "./renderReportEmail.js";

export async function sendReport(args: { key: string; email: string; report: EmailReport; manageUrl: string; provider: EmailProvider; ledger: DeliveryLedger }): Promise<"sent" | "duplicate"> {
  if (!await args.ledger.acquire(args.key)) return "duplicate";
  try {
    const rendered = renderReportEmail(args.report, args.manageUrl);
    const result = await args.provider.send({ to: args.email, ...rendered });
    await args.ledger.delivered(args.key, result.messageId);
    return "sent";
  } catch (error) {
    await args.ledger.failed(args.key, true);
    throw error;
  }
}
