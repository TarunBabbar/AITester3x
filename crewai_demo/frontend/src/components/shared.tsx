"use client";

import { useEffect, useState } from "react";
import type { AgentRow, CommandRow, EvalRow } from "@/lib/api";

// ---------------------------------------------------------------------------
// Shared UI building blocks
// ---------------------------------------------------------------------------
export function Card({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <section className={`rounded-xl border border-line bg-panel ${className}`}>{children}</section>;
}

export function CardTitle({ step, title, subtitle }: { step: string; title: string; subtitle?: string }) {
  return (
    <div className="flex items-center gap-3 border-b border-line px-5 py-4">
      <span className="grid size-6 shrink-0 place-items-center rounded-md bg-accent-soft font-mono text-[11px] font-bold text-accent">
        {step}
      </span>
      <div>
        <h2 className="text-sm font-semibold tracking-tight">{title}</h2>
        {subtitle && <p className="text-xs text-ink-soft">{subtitle}</p>}
      </div>
    </div>
  );
}

// DeepEval metric card — score, status, reasoning
export function EvalCard({ row }: { row: EvalRow }) {
  const running = row.state === "running";
  const score = row.result?.score;
  return (
    <li className="flex items-start gap-3 rounded-lg border border-accent/30 bg-accent-soft/20 px-3.5 py-3">
      <span
        className={`mt-0.5 size-2.5 shrink-0 rounded-full ${
          running ? "live-dot bg-warn" : row.state === "done" ? "bg-ok" : "bg-err"
        }`}
      />
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-medium">{row.title}</p>
          {score !== undefined && (
            <span
              className={`shrink-0 rounded-md border px-2 py-0.5 font-mono text-xs font-bold ${
                score >= 0.7
                  ? "border-ok/40 bg-ok-soft text-ok"
                  : "border-err/40 bg-err-soft text-err"
              }`}
            >
              {(score * 100).toFixed(0)}%
            </span>
          )}
        </div>
        <p className={`mt-0.5 text-xs ${running ? "animate-pulse text-warn" : "text-ink-soft"}`}>
          {running
            ? row.activity
            : row.state === "failed"
              ? row.result?.reason || "Evaluation failed"
              : row.result?.reason || row.activity}
        </p>
        {score !== undefined && row.result?.durationMs !== undefined && (
          <p className="mt-0.5 font-mono text-[10px] text-ink-soft">
            {row.result.status === "passed" ? "PASSED" : "FAILED"} ·{" "}
            {(row.result.durationMs / 1000).toFixed(1)}s
          </p>
        )}
      </div>
    </li>
  );
}

export function PriorityPill({ priority }: { priority: string }) {
  const styles: Record<string, string> = {
    P0: "border-err/40 bg-err-soft text-err",
    P1: "border-warn/40 bg-warn-soft text-warn",
    P2: "border-accent/40 bg-accent-soft text-accent",
    P3: "border-line bg-inset text-ink-soft",
  };
  return (
    <span className={`rounded border px-1.5 py-0.5 font-mono text-[10px] font-bold ${styles[priority] ?? styles.P3}`}>
      {priority}
    </span>
  );
}

export function StatusDot({ state }: { state: AgentRow["state"] }) {
  if (state === "running")
    return <span className="live-dot mt-1 block size-2.5 shrink-0 rounded-full bg-warn" />;
  if (state === "done")
    return <span className="mt-0.5 block size-2.5 shrink-0 rounded-full bg-ok" />;
  if (state === "failed")
    return <span className="mt-0.5 block size-2.5 shrink-0 rounded-full bg-err" />;
  if (state === "queued")
    return <span className="mt-0.5 block size-2.5 shrink-0 rounded-full bg-ink-soft/50" />;
  return <span className="mt-0.5 block size-2.5 shrink-0 rounded-full bg-line" />;
}

// Live elapsed timer for a running agent — re-renders on the 1s tick
export function LiveTimer({ startedAt }: { startedAt: number }) {
  const [, setNow] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);
  const s = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
  return <span className="shrink-0 font-mono text-[11px] text-warn">{s}s</span>;
}

export function AgentRowView({ row, output }: { row: AgentRow; output?: string }) {
  const running = row.state === "running";
  return (
    <li className="flex items-start gap-3 rounded-lg border border-line bg-inset px-3.5 py-3">
      <StatusDot state={row.state} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-3">
          <p className={`text-sm font-medium ${row.state === "idle" || row.state === "queued" ? "text-ink-soft" : ""}`}>
            {row.title}
          </p>
          {row.state === "queued" && (
            <span className="shrink-0 rounded border border-line bg-inset px-1.5 py-0.5 font-mono text-[10px] text-ink-soft">
              queued
            </span>
          )}
          {row.durationMs !== undefined && (
            <span className="shrink-0 font-mono text-[11px] text-ink-soft">
              {(row.durationMs / 1000).toFixed(1)}s
            </span>
          )}
          {running && row.startedAt && <LiveTimer startedAt={row.startedAt} />}
        </div>
        <p
          className={`mt-0.5 text-xs ${
            running
              ? "text-warn"
              : row.state === "failed"
                ? "text-err"
                : row.state === "done"
                  ? "text-ink-soft"
                  : row.state === "queued"
                    ? "text-ink-soft/60"
                    : "text-ink-soft/70"
          }`}
        >
          {row.state === "done" || row.state === "failed" ? row.detail : row.activity}
        </p>

        {/* Per-agent output, expandable once the agent has produced something */}
        {output && (
          <details className="mt-2">
            <summary className="cursor-pointer font-mono text-[11px] text-accent">
              show output
            </summary>
            <pre className="mt-1.5 max-h-60 overflow-auto rounded-md border border-line bg-panel p-3 font-mono text-[11px] leading-relaxed whitespace-pre-wrap text-ink/80">
              {output}
            </pre>
          </details>
        )}
      </div>
    </li>
  );
}

export function CommandRowView({ row }: { row: CommandRow }) {
  const done = row.state === "done";
  return (
    <li className="flex items-center justify-between gap-3 rounded-lg border border-line bg-inset px-3.5 py-2.5 font-mono text-xs">
      <span className="min-w-0 truncate text-ink-soft">
        <span className="text-accent">$</span> {row.command}
      </span>
      <span
        className={`shrink-0 ${
          done ? (row.exitCode === 0 ? "text-ok" : "text-err") : "text-warn"
        }`}
      >
        {done
          ? `exit ${row.exitCode} · ${((row.durationMs ?? 0) / 1000).toFixed(1)}s`
          : "running…"}
      </span>
    </li>
  );
}

export function TerminalBlock({ commands }: { commands: CommandRow[] }) {
  return (
    <div className="rounded-lg border border-line bg-panel">
      <div className="flex items-center gap-1.5 border-b border-line px-3.5 py-2">
        <span className="size-2 rounded-full bg-err/70" />
        <span className="size-2 rounded-full bg-warn/70" />
        <span className="size-2 rounded-full bg-ok/70" />
        <span className="ml-2 font-mono text-[11px] text-ink-soft">terminal</span>
      </div>
      <ol className="flex flex-col gap-1 p-3">
        {commands.map((row, i) => (
          <CommandRowView key={`${row.command}-${i}`} row={row} />
        ))}
      </ol>
    </div>
  );
}

// Horizontal stage stepper. Different views show different stages:
//   Pipeline:   Read -> Cases
//   Automation: POM -> Framework -> Run
export type Stage = { key: number; label: string; agent: string | null };

export const STAGES: Stage[] = [
  { key: 1, label: "Read", agent: "page_reader" },
  { key: 2, label: "Cases", agent: "test_designer" },
  { key: 3, label: "POM", agent: "pom_writer" },
  { key: 4, label: "Framework", agent: "framework_architect" },
  { key: 5, label: "Run", agent: null },
];

export function StageStepper({
  stages,
  agents,
  evals,
  commands,
}: {
  stages: Stage[];
  agents: AgentRow[];
  evals?: EvalRow[];
  commands: CommandRow[];
}) {
  const stateOf = (agentKey: string | null): AgentRow["state"] | EvalRow["state"] => {
    if (!agentKey) {
      if (commands.some((c) => c.state === "running")) return "running";
      if (commands.length > 0) return "done";
      return "idle";
    }
    // Eval stages resolve from the evals list; agent stages from agents
    if (evals?.some((e) => e.key === agentKey)) {
      return evals.find((e) => e.key === agentKey)!.state;
    }
    return agents.find((a) => a.key === agentKey)?.state ?? "idle";
  };

  const dot = (state: string) =>
    state === "running" ? (
      <span className="live-dot mx-auto block size-3 rounded-full bg-warn" />
    ) : state === "done" ? (
      <span className="mx-auto grid size-3 place-items-center rounded-full bg-ok text-[8px] font-bold text-white">✓</span>
    ) : state === "failed" ? (
      <span className="mx-auto grid size-3 place-items-center rounded-full bg-err text-[8px] font-bold text-white">✕</span>
    ) : (
      <span className="mx-auto block size-3 rounded-full bg-line" />
    );

  return (
    <div className="flex items-start">
      {stages.map((stage, i) => {
        const st = stateOf(stage.agent);
        return (
          <div key={stage.key} className="flex flex-1 items-start">
            <div className="flex w-full flex-col items-center gap-1.5">
              {dot(st)}
              <span
                className={`text-[11px] font-medium ${
                  st === "running"
                    ? "text-warn"
                    : st === "done"
                      ? "text-ok"
                      : st === "failed"
                        ? "text-err"
                        : "text-ink-soft"
                }`}
              >
                {stage.label}
              </span>
            </div>
            {i < stages.length - 1 && <div className="mt-[5px] h-px flex-1 bg-line" />}
          </div>
        );
      })}
    </div>
  );
}
