"use client";

import { useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { AgentRow, CommandRow, EvalRow, TestCase } from "@/lib/api";
import {
  Card,
  CardTitle,
  StageStepper,
  AgentRowView,
  TerminalBlock,
  EvalCard,
  Stage,
} from "@/components/shared";

// Styled markdown components for the RI display
const mdComponents = {
  h1: (props: React.HTMLAttributes<HTMLHeadingElement>) => (
    <h1 className="mb-2 mt-4 text-base font-bold text-ink first:mt-0" {...props} />
  ),
  h2: (props: React.HTMLAttributes<HTMLHeadingElement>) => (
    <h2 className="mb-2 mt-5 border-b border-line pb-1 text-sm font-bold uppercase tracking-wide text-accent first:mt-0" {...props} />
  ),
  h3: (props: React.HTMLAttributes<HTMLHeadingElement>) => (
    <h3 className="mb-1.5 mt-4 text-sm font-semibold text-ink first:mt-0" {...props} />
  ),
  p: (props: React.HTMLAttributes<HTMLParagraphElement>) => (
    <p className="mb-2 text-sm leading-relaxed text-ink" {...props} />
  ),
  ul: (props: React.HTMLAttributes<HTMLUListElement>) => (
    <ul className="mb-2 list-disc space-y-1 pl-5 text-sm text-ink" {...props} />
  ),
  ol: (props: React.HTMLAttributes<HTMLOListElement>) => (
    <ol className="mb-2 list-decimal space-y-1 pl-5 text-sm text-ink" {...props} />
  ),
  li: (props: React.LiHTMLAttributes<HTMLLIElement>) => (
    <li className="leading-relaxed" {...props} />
  ),
  strong: (props: React.HTMLAttributes<HTMLElement>) => (
    <strong className="font-semibold text-ink" {...props} />
  ),
  em: (props: React.HTMLAttributes<HTMLElement>) => (
    <em className="italic text-ink/90" {...props} />
  ),
  code: (props: React.HTMLAttributes<HTMLElement>) => (
    <code className="rounded bg-inset px-1 py-0.5 font-mono text-xs text-accent-deep" {...props} />
  ),
  pre: (props: React.HTMLAttributes<HTMLPreElement>) => (
    <pre className="mb-2 overflow-auto rounded-md border border-line bg-inset p-3 font-mono text-xs leading-relaxed" {...props} />
  ),
  table: (props: React.HTMLAttributes<HTMLTableElement>) => (
    <div className="mb-2 overflow-auto rounded-md border border-line">
      <table className="w-full border-collapse text-left text-xs" {...props} />
    </div>
  ),
  thead: (props: React.HTMLAttributes<HTMLTableSectionElement>) => (
    <thead className="bg-inset text-ink-soft" {...props} />
  ),
  th: (props: React.ThHTMLAttributes<HTMLTableCellElement>) => (
    <th className="border-b border-line px-2.5 py-1.5 font-semibold text-ink" {...props} />
  ),
  td: (props: React.TdHTMLAttributes<HTMLTableCellElement>) => (
    <td className="border-b border-line/60 px-2.5 py-1.5 text-ink" {...props} />
  ),
  blockquote: (props: React.HTMLAttributes<HTMLQuoteElement>) => (
    <blockquote className="mb-2 border-l-2 border-accent/50 pl-3 text-sm italic text-ink-soft" {...props} />
  ),
  hr: () => <hr className="my-3 border-line" />,
};

// Test Case Generation pipeline stages:
// RI -> Coverage Eval -> Test Cases (reads URL + designs) -> Cases Eval
const PIPELINE_STAGES: Stage[] = [
  { key: 1, label: "RI", agent: "ri" },
  { key: 2, label: "Coverage Eval", agent: "coverage_eval" },
  { key: 3, label: "Test Cases", agent: "test_designer" },
  { key: 4, label: "Cases Eval", agent: "cases_eval" },
];

// True execution order for the interleaved timeline
const PIPELINE_ORDER = ["ri", "coverage_eval", "test_designer", "cases_eval"];

/**
 * Test Case Generation — the working view. Shows the Requirement
 * Intelligence step, DeepEval coverage checks, the test case designer, and
 * the generated test cases as a selectable list.
 */
export default function PipelineView({
  phaseLabel,
  agents,
  evals,
  commands,
  tests,
  selected,
  riText,
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
  evals: EvalRow[];
  commands: CommandRow[];
  tests: TestCase[];
  selected: Set<string>;
  riText: string;
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
  const [riOpen, setRiOpen] = useState(true);

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

      {/* Agent + evaluation pipeline */}
      <Card>
        <CardTitle step="⌁" title="Crew at work" subtitle="Live status of each stage" />
        <div className="flex flex-col gap-4 px-5 py-4">
          <StageStepper stages={PIPELINE_STAGES} agents={agents} evals={evals} commands={commands} />
          <ol className="flex flex-col gap-2">
            {/* Render agents + evals in true pipeline order, interleaved.
                Stages with no data yet show a "waiting" placeholder so the
                full RI -> Coverage Eval -> Test Cases -> Cases Eval flow is
                always visible. */}
            {PIPELINE_ORDER.map((key) => {
              const agent = agents.find((a) => a.key === key && a.key !== "page_reader");
              if (agent) {
                return <AgentRowView key={agent.key} row={agent} output={outputs[agent.key]} />;
              }
              const evalRow = evals.find((e) => e.key === key);
              if (evalRow) {
                return <EvalCard key={evalRow.key} row={evalRow} />;
              }
              // Not started yet — show a waiting placeholder in the right slot
              return (
                <li
                  key={key}
                  className="flex items-start gap-3 rounded-lg border border-dashed border-line bg-panel px-3.5 py-3"
                >
                  <span className="mt-0.5 block size-2.5 shrink-0 rounded-full bg-line" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink-soft">
                      {key === "coverage_eval"
                        ? "Coverage Evaluation · DeepEval"
                        : key === "cases_eval"
                          ? "Test Case Evaluation · DeepEval"
                          : "Waiting…"}
                    </p>
                    <p className="mt-0.5 text-xs text-ink-soft/60">
                      {key === "coverage_eval"
                        ? "Will cross-verify the RI against the user's requirements"
                        : key === "cases_eval"
                          ? "Will cross-verify the test cases against the RI"
                          : "Waiting for the previous stage…"}
                    </p>
                  </div>
                </li>
              );
            })}
          </ol>
          {commands.length > 0 && <TerminalBlock commands={commands} />}
        </div>
      </Card>

      {error && (
        <div className="rounded-lg border border-err/30 bg-err-soft px-4 py-3 text-sm whitespace-pre-wrap text-err">
          {error}
        </div>
      )}

      {/* Requirement Intelligence — rendered as proper markdown */}
      {riText && (
        <Card>
          <CardTitle step="RI" title="Requirement Intelligence" subtitle="Goals, acceptance criteria, edge cases and risks" />
          <div className="px-5 py-4">
            <button
              onClick={() => setRiOpen((o) => !o)}
              className="mb-3 flex items-center gap-2 rounded-md border border-line bg-inset px-3 py-1.5 text-xs font-medium text-ink-soft transition hover:border-accent/40"
            >
              <span>{riOpen ? "▾" : "▸"}</span>
              {riOpen ? "Collapse" : "Expand"} the requirement intelligence
            </button>
            {riOpen && (
              <div className="max-h-[30rem] overflow-auto rounded-lg border border-line bg-panel p-4">
                <ReactMarkdown remarkPlugins={[remarkGfm]} components={mdComponents}>
                  {riText}
                </ReactMarkdown>
              </div>
            )}
          </div>
        </Card>
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
