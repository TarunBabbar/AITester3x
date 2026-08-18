import { env } from "@/lib/env";

export interface ChatMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

export interface CallOptions {
  model?: string;
  temperature?: number;
  maxTokens?: number;
}

async function sleep(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}

export async function callOpenRouter(
  messages: ChatMessage[],
  opts?: CallOptions
): Promise<string> {
  const model = opts?.model || env.OPENROUTER_MODEL;
  const maxRetries = env.OPENROUTER_MAX_RETRIES;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    const controller = new AbortController();
    const timeout = setTimeout(
      () => controller.abort(),
      env.OPENROUTER_TIMEOUT_MS
    );
    try {
      const res = await fetch(
        `${env.OPENROUTER_BASE_URL}/chat/completions`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            model,
            messages,
            temperature: opts?.temperature ?? env.OPENROUTER_TEMPERATURE,
            max_tokens: opts?.maxTokens ?? env.OPENROUTER_MAX_TOKENS,
          }),
          signal: controller.signal,
        }
      );
      if (!res.ok) {
        const body = await res.text();
        if (res.status === 429 || res.status >= 500) {
          const delay = Math.min(1000 * 2 ** attempt, 15000);
          console.warn(
            `OpenRouter attempt ${attempt + 1} failed (${res.status}), retrying in ${delay}ms`
          );
          await sleep(delay);
          continue;
        }
        throw new Error(`OpenRouter error ${res.status}: ${body}`);
      }
      const data = await res.json();
      return data.choices[0].message.content as string;
    } catch (e) {
      if (attempt === maxRetries) {
        throw e;
      }
      const delay = Math.min(1000 * 2 ** attempt, 15000);
      console.warn(`OpenRouter call error, retrying in ${delay}ms: ${e}`);
      await sleep(delay);
    } finally {
      clearTimeout(timeout);
    }
  }
  throw new Error("OpenRouter call exhausted retries");
}
