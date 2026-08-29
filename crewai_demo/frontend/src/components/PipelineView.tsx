"use client";

import { AgentRow, CommandRow, TestCase } from "@/lib/api";
import { Card, CardTitle, StageStepper, AgentRowView, TerminalBlock } from "@/components/shared";

/**
 * Test Case Generation — the working view. Shows the four agents at work
 * (stage stepper + agent timeline) and, once finished, the generated test
 * cases as a selectable list that hands off to the automation view.
 */
export default function PipelineView({
  phaseLabel,
  agents,
  commands,
  tests,
  selected,
  onToggleTest,
  onSelectAll,
  onAutomate,
  onStop,
  busy,
  error,
  outputs,
}: {
  phaseLabel: string;
  agents: AgentRow[];
  commands: CommandRow[];
  tests: TestCase[];
  selected: Set<string>;
  onToggleTest: (id: string) => void;
  onSelectAll: () => void;
  onAutomate: () => void;
  onStop: () => void;
  busy: boolean;
  error: string;
  outputs: Record<string, string>;
}) {
  const anyAgentRunning = agents.some((a) => a.state === "running");
  const anyCommandRunning = commands.some((c) => c.state === "running");

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold tracking-tight">Test Case Generation</h1>
          <p className="mt-0.5 text-sm text-ink-soft">
            {phaseLabel || "The crew is standing by — start a run from Home."}
          </p>
        </div>
        {(anyAgentRunning || anyCommandRunning) && (
          <button
            onClick={onStop}
            className="shrink-0 rounded-lg border border-err/40 bg-err-soft px-4 py-2 text-sm font-semibold text-err transition hover:bg-err/20"
          >
            ■ Stop
          </button>
        )}
      </div>

      {/* Agent pipeline */}
      <Card>
        <CardTitle step="⌁" title="Crew at work" subtitle="Live status of each agent" />
        <div className="flex flex-col gap-4 px-5 py-4">
          <StageStepper agents={agents} commands={commands} />
          <ol className="flex flex-col gap-2">
            {agents.map((row) => (
              <AgentRowView key={row.key} row={row} output={outputs[row.key]} />
            ))}
          </ol>
          {commands.length > 0 && <TerminalBlock commands={commands} />}
        </div>
      </Card>

      {error && (
        <div className="rounded-lg border border-err/30 bg-err-soft px-4 py-3 text-sm whitespace-pre-wrap text-err">
          {error}
        </div>
      )}

      {/* Generated test cases */}
      {tests.length > 0 && (
        <Card>
          <CardTitle
            step="✓"
            title="Generated test cases"
            subtitle={`${selected.size} of ${tests.length} selected`}
          />
          <div className="flex flex-col gap-2 px-5 py-4">
            {tests.map((tc) => (
              <label
                key={tc.id}
                className={`flex cursor-pointer gap-3 rounded-lg border px-3.5 py-3 transition ${
                  selected.has(tc.id)
                    ? "border-accent/40 bg-accent-soft/40"
                    : "border-line bg-inset hover:border-line-soft"
                }`}
              >
                <input
                  type="checkbox"
                  checked={selected.has(tc.id)}
                  onChange={() => onToggleTest(tc.id)}
                  className="mt-1 size-4 accent-accent"
                />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-[11px] font-bold text-accent">{tc.id}</span>
                    <span className="text-sm font-medium">{tc.title}</span>
                    <span className="rounded border border-line bg-inset px-1.5 py-0.5 font-mono text-[10px] font-bold text-ink-soft">
                      {tc.priority}
                    </span>
                  </div>
                  {tc.steps?.length > 0 && (
                    <ol className="mt-1 list-inside list-decimal text-xs text-ink-soft">
                      {tc.steps.map((s, i) => (
                        <li key={i}>{s}</li>
                      ))}
                    </ol>
                  )}
                  {tc.expected && (
                    <p className="mt-1 text-xs text-ink-soft">
                      <span className="font-semibold text-ok">Expected:</span> {tc.expected}
                    </p>
                  )}
                </div>
              </label>
            ))}

            <div className="mt-2 flex gap-2.5">
              <button
                onClick={onSelectAll}
                disabled={busy}
                className="rounded-lg border border-line px-4 py-2 text-sm font-medium transition hover:border-accent/50 disabled:opacity-50"
              >
                {selected.size === tests.length ? "Deselect All" : "Select All"}
              </button>
              <button
                onClick={onAutomate}
                disabled={busy || selected.size === 0}
                className="flex-1 rounded-lg bg-accent px-5 py-2 text-sm font-semibold text-white transition hover:bg-accent-deep disabled:opacity-50"
              >
                Automate Selected ({selected.size})
              </button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
