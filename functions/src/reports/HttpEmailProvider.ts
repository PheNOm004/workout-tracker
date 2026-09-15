import type { EmailMessage, EmailProvider } from "./EmailProvider.js";

export class HttpEmailProvider implements EmailProvider {
  private static readonly REQUEST_TIMEOUT_MILLIS = 15_000;
  constructor(private readonly endpoint: string, private readonly apiKey: string, private readonly from: string) {}
  async send(message: EmailMessage): Promise<{ messageId: string }> {
    const response = await fetch(this.endpoint, {
      method: "POST",
      headers: { "authorization": `Bearer ${this.apiKey}`, "content-type": "application/json" },
      body: JSON.stringify({ from: this.from, ...message }),
      signal: AbortSignal.timeout(HttpEmailProvider.REQUEST_TIMEOUT_MILLIS),
    });
    if (!response.ok) throw new Error(`email provider rejected request with status ${response.status}`);
    const body = await response.json() as { id?: string; messageId?: string };
    return { messageId: body.messageId ?? body.id ?? `accepted-${Date.now()}` };
  }
}
