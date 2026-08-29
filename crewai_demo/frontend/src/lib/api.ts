// API client for the CrewAI QA backend.
// Base URL is configurable at build time via NEXT_PUBLIC_API_BASE.
export const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://127.0.0.1:8000";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------
export type AgentKey = "page_reader" | "test_designer" | "pom_writer" | "framework_architect";

export interface AgentRow {
  key: string;
  title: string;
  activity: string;
  state: "idle" | "running" | "done" | "failed";
  detail?: string;
  durationMs?: number;
  startedAt?: number; // epoch ms — set when the agent flips to running
}

export interface TestCase {
  id: string;
  title: string;
  priority: string;
  preconditions: string;
  steps: string[];
  expected: string;
}

export interface CommandRow {
  command: string;
  state: "running" | "done";
  exitCode?: number;
  durationMs?: number;
}

export interface PipelineEvent {
  type:
    | "run_started"
    | "agent_started"
    | "agent_activity"
    | "agent_done"
    | "agent_failed"
    | "command_started"
    | "command_done"
    | "phase_complete"
    | "error";
  runId?: string;
  phase?: number;
  key?: string;
  title?: string;
  activity?: string;
  detail?: string;
  durationMs?: number;
  command?: string;
  exitCode?: number;
  message?: string;
  payload?: Record<string, unknown>;
}

export interface Health {
  status: string;
  node: { available: boolean; detail: string };
  npm: { available: boolean };
}

export interface OutputFile {
  name: string;
  content: string;
}

// ---------------------------------------------------------------------------
// Plain JSON helpers
// ---------------------------------------------------------------------------
export async function getHealth(): Promise<Health> {
  const res = await fetch(`${API_BASE}/api/health`);
  return res.json();
}

export async function getConfig(): Promise<{ provider: string; model: string }> {
  const res = await fetch(`${API_BASE}/api/config`);
  return res.json();
}

export async function listOutputFiles(): Promise<OutputFile[]> {
  const res = await fetch(`${API_BASE}/api/output`);
  const data = await res.json();
  return data.files as OutputFile[];
}

// ---------------------------------------------------------------------------
// SSE over POST — the pipeline endpoints stream Server-Sent Events, but
// EventSource only supports GET, so we parse the stream from fetch().
// ---------------------------------------------------------------------------
export async function streamPipeline(
  path: string,
  body: Record<string, unknown>,
  onEvent: (event: PipelineEvent) => void,
): Promise<void> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok || !res.body) {
    throw new Error(`Backend returned ${res.status}`);
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    let sep: number;
    while ((sep = buffer.indexOf("\n\n")) >= 0) {
      const chunk = buffer.slice(0, sep);
      buffer = buffer.slice(sep + 2);
      for (const line of chunk.split("\n")) {
        if (line.startsWith("data: ")) {
          onEvent(JSON.parse(line.slice(6)) as PipelineEvent);
        }
      }
    }
  }
}
