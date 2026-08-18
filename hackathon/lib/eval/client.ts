import { env } from "@/lib/env";

export interface EvalResult {
  agent_name: string;
  metric_name: string;
  score: number;
  threshold: number;
  passed: boolean;
  reasoning: string;
}

export interface EvalRequest {
  pipeline_run_id: string;
  agent_name: string;
  criteria: string;
  input: string;
  output: string;
}

/**
 * Calls the DeepEval Python microservice. Returns null if the service is
 * unreachable so callers can decide whether to block or log.
 */
export async function runEval(
  request: EvalRequest
): Promise<EvalResult | null> {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), env.EVAL_TIMEOUT_MS);
    const res = await fetch(`${env.EVAL_SERVICE_URL}/evaluate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
      signal: controller.signal,
    });
    clearTimeout(timeout);
    if (!res.ok) {
      console.warn(`Eval service error ${res.status}: ${await res.text()}`);
      return null;
    }
    return (await res.json()) as EvalResult;
  } catch (e) {
    console.warn(`Eval service unreachable: ${e}`);
    return null;
  }
}
