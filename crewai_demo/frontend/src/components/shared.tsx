"use client";

import { useEffect, useState } from "react";
import type { AgentRow, CommandRow } from "@/lib/api";

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
          <p className={`text-sm font-medium ${row.state === "idle" ? "text-ink-soft" : ""}`}>
            {row.title}
          </p>
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
            <pre className="mt-1.5 max-h-60 overflow-auto rounded-md border border-line bg-[#070b12] p-3 font-mono text-[11px] leading-relaxed whitespace-pre-wrap">
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
    <div className="rounded-lg border border-line bg-[#070b12]">
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

// Horizontal stage stepper: Read -> Cases -> POM -> Framework -> Run
export const STAGES: { key: number; label: string; agent: string | null }[] = [
  { key: 1, label: "Read", agent: "page_reader" },
  { key: 2, label: "Cases", agent: "test_designer" },
  { key: 3, label: "POM", agent: "pom_writer" },
  { key: 4, label: "Framework", agent: "framework_architect" },
  { key: 5, label: "Run", agent: null },
];

export function StageStepper({ agents, commands }: { agents: AgentRow[]; commands: CommandRow[] }) {
  const stateOf = (agentKey: string | null): AgentRow["state"] => {
    if (!agentKey) {
      if (commands.some((c) => c.state === "running")) return "running";
      if (commands.length > 0) return "done";
      return "idle";
    }
    return agents.find((a) => a.key === agentKey)?.state ?? "idle";
  };

  const dot = (state: AgentRow["state"]) =>
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
      {STAGES.map((stage, i) => {
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
            {i < STAGES.length - 1 && <div className="mt-[5px] h-px flex-1 bg-line" />}
          </div>
        );
      })}
    </div>
  );
}
