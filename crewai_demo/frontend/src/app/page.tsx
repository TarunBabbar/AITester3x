"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  AgentRow,
  CommandRow,
  getConfig,
  getHealth,
  Health,
  listOutputFiles,
  OutputFile,
  streamPipeline,
  TestCase,
} from "@/lib/api";

// ---------------------------------------------------------------------------
// Small presentational pieces
// ---------------------------------------------------------------------------
function Card({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <section
      className={`rounded-xl border border-line bg-panel ${className}`}
    >
      {children}
    </section>
  );
}

function CardTitle({ step, title, subtitle }: { step: string; title: string; subtitle?: string }) {
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

function PriorityPill({ priority }: { priority: string }) {
  const styles: Record<string, string> = {
    P0: "border-err/40 bg-err-soft text-err",
    P1: "border-warn/40 bg-warn-soft text-warn",
    P2: "border-accent/40 bg-accent-soft text-accent",
    P3: "border-line bg-inset text-ink-soft",
  };
  return (
    <span
      className={`rounded border px-1.5 py-0.5 font-mono text-[10px] font-bold ${styles[priority] ?? styles.P3}`}
    >
      {priority}
    </span>
  );
}

function StatusDot({ state }: { state: "running" | "done" | "failed" }) {
  if (state === "running")
    return (
      <span className="live-dot mt-1 block size-2.5 rounded-full bg-warn" />
    );
  if (state === "done")
    return <span className="mt-0.5 block size-2.5 rounded-full bg-ok" />;
  return <span className="mt-0.5 block size-2.5 rounded-full bg-err" />;
}

function AgentRowView({ row }: { row: AgentRow }) {
  return (
    <li className="flex gap-3 rounded-lg border border-line bg-inset px-3.5 py-3">
      <StatusDot state={row.state} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-medium">{row.title}</p>
          {row.durationMs !== undefined && (
            <span className="shrink-0 font-mono text-[11px] text-ink-soft">
              {(row.durationMs / 1000).toFixed(1)}s
            </span>
          )}
        </div>
        <p
          className={`mt-0.5 text-xs ${
            row.state === "running"
              ? "text-warn"
              : row.state === "failed"
                ? "text-err"
                : "text-ink-soft"
          }`}
        >
          {row.state === "done" || row.state === "failed" ? row.detail : row.activity}
        </p>
      </div>
    </li>
  );
}

function CommandRowView({ row }: { row: CommandRow }) {
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

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------
export default function Home() {
  // config
  const [health, setHealth] = useState<Health | null>(null);
  const [provider, setProvider] = useState("");
  const [model, setModel] = useState("");

  // phase 1 inputs
  const [url, setUrl] = useState("https://saucedemo.com");
  const [requirements, setRequirements] = useState("");

  // pipeline visibility
  const [phaseLabel, setPhaseLabel] = useState("");
  const [agents, setAgents] = useState<AgentRow[]>([]);
  const [commands, setCommands] = useState<CommandRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  // data
  const [runId, setRunId] = useState("");
  const [tests, setTests] = useState<TestCase[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [pomCode, setPomCode] = useState("");
  const [frameworkFiles, setFrameworkFiles] = useState<OutputFile[]>([]);
  const [runOutput, setRunOutput] = useState("");
  const [runSuccess, setRunSuccess] = useState<boolean | null>(null);
  const [testsReady, setTestsReady] = useState(false);
  const [frameworkReady, setFrameworkReady] = useState(false);

  // tabs
  const [tab, setTab] = useState<"pom" | "files" | "run">("pom");

  // ticking timer while an agent is running
  const [, setTick] = useState(0);
  const anyRunning =
    agents.some((a) => a.state === "running") ||
    commands.some((c) => c.state === "running");
  useEffect(() => {
    if (!anyRunning) return;
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, [anyRunning]);

  useEffect(() => {
    getHealth().then(setHealth).catch(() => setHealth(null));
    getConfig()
      .then((c) => {
        setProvider(c.provider);
        setModel(c.model);
      })
      .catch(() => {});
  }, []);

  const upsertAgent = useCallback(
    (patch: Partial<AgentRow> & { key: string }) => {
      setAgents((rows) => {
        const idx = rows.findIndex((r) => r.key === patch.key);
        if (idx === -1) return rows;
        const next = [...rows];
        next[idx] = { ...next[idx], ...patch } as AgentRow;
        return next;
      });
    },
    [],
  );

  const startPhase = useCallback((label: string, rows: AgentRow[]) => {
    setError("");
    setPhaseLabel(label);
    setAgents(rows);
    setCommands([]);
    setBusy(true);
  }, []);

  const handleError = useCallback((message: string) => {
    setError(message);
    setAgents((rows) =>
      rows.map((r) =>
        r.state === "running" ? { ...r, state: "failed", detail: message } : r,
      ),
    );
  }, []);

  // -------------------------------------------------------------------------
  // Phase 1 — generate test cases
  // -------------------------------------------------------------------------
  async function generateTests() {
    if (!url.trim()) {
      setError("Enter a page URL first.");
      return;
    }
    startPhase("Phase 1 · Generate Test Cases", [
      {
        key: "page_reader",
        title: "Agent 1 · Page Reader",
        activity: "Waiting to start…",
        state: "running",
      },
      {
        key: "test_designer",
        title: "Agent 2 · Test Case Designer",
        activity: "Queued",
        state: "running",
      },
    ]);
    setTests([]);
    setSelected(new Set());
    setTestsReady(false);

    try {
      await streamPipeline(
        "/api/generate-tests/stream",
        { url: url.trim(), requirements: requirements.trim() },
        (ev) => {
          if (ev.type === "run_started") setRunId(ev.runId!);
          if (ev.type === "agent_activity")
            upsertAgent({ key: ev.key!, activity: ev.activity! });
          if (ev.type === "agent_started")
            upsertAgent({ key: ev.key!, activity: ev.activity ?? "Working…" });
          if (ev.type === "agent_done")
            upsertAgent({
              key: ev.key!,
              state: "done",
              detail: ev.detail,
              durationMs: ev.durationMs,
            });
          if (ev.type === "agent_failed")
            upsertAgent({ key: ev.key!, state: "failed", detail: ev.detail });
          if (ev.type === "phase_complete") {
            const payload = ev.payload as { run_id: string; test_cases: TestCase[] };
            setTests(payload.test_cases);
            setSelected(new Set(payload.test_cases.map((t) => t.id)));
            setTestsReady(true);
          }
          if (ev.type === "error") handleError(ev.message!);
        },
      );
    } catch (err) {
      handleError(
        `Backend unreachable — is it running on port 8000? (${(err as Error).message})`,
      );
    } finally {
      setBusy(false);
    }
  }

  // -------------------------------------------------------------------------
  // Phase 2 — automate selected test cases
  // -------------------------------------------------------------------------
  async function automateSelected() {
    if (!selected.size) {
      setError("Select at least one test case to automate.");
      return;
    }
    startPhase("Phase 2 · Automate Selected Test Cases", [
      {
        key: "pom_writer",
        title: "Agent 3 · POM Writer",
        activity: "Waiting to start…",
        state: "running",
      },
      {
        key: "framework_architect",
        title: "Agent 4 · Framework Architect",
        activity: "Queued",
        state: "running",
      },
    ]);
    setPomCode("");
    setFrameworkFiles([]);
    setRunOutput("");
    setRunSuccess(null);
    setFrameworkReady(false);

    try {
      await streamPipeline(
        "/api/automate/stream",
        { run_id: runId, selected: Array.from(selected) },
        (ev) => {
          if (ev.type === "agent_started")
            upsertAgent({ key: ev.key!, activity: ev.activity ?? "Working…" });
          if (ev.type === "agent_done")
            upsertAgent({
              key: ev.key!,
              state: "done",
              detail: ev.detail,
              durationMs: ev.durationMs,
            });
          if (ev.type === "agent_failed")
            upsertAgent({ key: ev.key!, state: "failed", detail: ev.detail });
          if (ev.type === "phase_complete") {
            const payload = ev.payload as {
              pom_code: string;
              framework_files: string[];
            };
            setPomCode(payload.pom_code);
            setFrameworkReady(true);
            setTab("pom");
            listOutputFiles()
              .then((files) =>
                setFrameworkFiles(
                  files.filter((f) => f.name.startsWith(`${runId}/`)),
                ),
              )
              .catch(() => {});
          }
          if (ev.type === "error") handleError(ev.message!);
        },
      );
    } catch (err) {
      handleError(
        `Backend unreachable — is it running on port 8000? (${(err as Error).message})`,
      );
    } finally {
      setBusy(false);
    }
  }

  // -------------------------------------------------------------------------
  // Phase 2b — run the generated tests
  // -------------------------------------------------------------------------
  async function runTests() {
    startPhase("Phase 2b · Run Generated Tests", []);
    try {
      await streamPipeline(
        "/api/run-tests/stream",
        { run_id: runId },
        (ev) => {
          if (ev.type === "command_started")
            setCommands((rows) => [
              ...rows,
              { command: ev.command!, state: "running" },
            ]);
          if (ev.type === "command_done")
            setCommands((rows) =>
              rows.map((r) =>
                r.command === ev.command
                  ? {
                      ...r,
                      state: "done",
                      exitCode: ev.exitCode,
                      durationMs: ev.durationMs,
                    }
                  : r,
              ),
            );
          if (ev.type === "phase_complete") {
            const payload = ev.payload as { success: boolean; output: string };
            setRunSuccess(payload.success);
            setRunOutput(payload.output);
            setTab("run");
          }
          if (ev.type === "error") handleError(ev.message!);
        },
      );
    } catch (err) {
      handleError(
        `Backend unreachable — is it running on port 8000? (${(err as Error).message})`,
      );
    } finally {
      setBusy(false);
    }
  }

  const inputDisabled = busy;

  return (
    <div className="page-glow min-h-screen">
      {/* ------------------------------------------------------------------ */}
      <header className="sticky top-0 z-10 border-b border-line bg-panel/85 backdrop-blur">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-5 py-3.5">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-lg bg-accent-soft text-base text-accent">
              ⚙
            </span>
            <div>
              <h1 className="text-[15px] font-bold tracking-tight">
                CrewAI QA Studio
              </h1>
              <p className="text-[11px] text-ink-soft">
                Read a page → generate tests → automate → run
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-[11px]">
            {model && (
              <span className="hidden rounded-md border border-line bg-inset px-2.5 py-1 font-mono text-ink-soft sm:inline">
                {provider} · {model}
              </span>
            )}
            <span
              className={`flex items-center gap-1.5 rounded-md border px-2.5 py-1 ${
                health?.status === "ok"
                  ? "border-ok/30 bg-ok-soft text-ok"
                  : "border-err/30 bg-err-soft text-err"
              }`}
            >
              <span
                className={`size-1.5 rounded-full ${
                  health?.status === "ok" ? "bg-ok" : "bg-err"
                }`}
              />
              {health
                ? `${health.node?.available ? "node ✓" : "node ✗"} ${
                    health.npm?.available ? "npm ✓" : "npm ✗"
                  }`
                : "offline"}
            </span>
          </div>
        </div>
      </header>

      <main className="mx-auto flex max-w-4xl flex-col gap-4 px-5 py-6">
        {/* Step 1 — inputs ------------------------------------------------ */}
        <Card>
          <CardTitle
            step="1"
            title="Point me at a page"
            subtitle="The Page Reader agent opens it in a real browser"
          />
          <div className="flex flex-col gap-3.5 px-5 py-4">
            <div>
              <label className="mb-1 block text-[11px] font-semibold uppercase tracking-wider text-ink-soft">
                Page URL
              </label>
              <input
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://saucedemo.com"
                disabled={inputDisabled}
                className="w-full rounded-lg border border-line bg-inset px-3.5 py-2.5 text-sm outline-none transition focus:border-accent disabled:opacity-50"
              />
            </div>
            <div>
              <label className="mb-1 block text-[11px] font-semibold uppercase tracking-wider text-ink-soft">
                Extra requirements{" "}
                <span className="font-normal normal-case">(optional)</span>
              </label>
              <textarea
                value={requirements}
                onChange={(e) => setRequirements(e.target.value)}
                rows={2}
                placeholder="e.g. Focus on negative login cases and validation messages."
                disabled={inputDisabled}
                className="w-full resize-none rounded-lg border border-line bg-inset px-3.5 py-2.5 text-sm outline-none transition focus:border-accent disabled:opacity-50"
              />
            </div>
            <button
              onClick={generateTests}
              disabled={inputDisabled}
              className="rounded-lg bg-accent px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-accent-deep disabled:opacity-50"
            >
              {busy && phaseLabel.includes("Phase 1")
                ? "Generating…"
                : "Generate Test Cases"}
            </button>
          </div>
        </Card>

        {/* Live pipeline -------------------------------------------------- */}
        {(agents.length > 0 || commands.length > 0) && (
          <Card>
            <CardTitle
              step="⌁"
              title={phaseLabel || "Pipeline"}
              subtitle="Live view of what the crew is doing"
            />
            <div className="flex flex-col gap-3 px-5 py-4">
              {agents.length > 0 && (
                <ol className="flex flex-col gap-2">
                  {agents.map((row) => (
                    <AgentRowView key={row.key} row={row} />
                  ))}
                </ol>
              )}
              {commands.length > 0 && (
                <ol className="flex flex-col gap-2">
                  {commands.map((row, i) => (
                    <CommandRowView key={`${row.command}-${i}`} row={row} />
                  ))}
                </ol>
              )}
            </div>
          </Card>
        )}

        {error && (
          <div className="rounded-lg border border-err/30 bg-err-soft px-4 py-3 text-sm whitespace-pre-wrap text-err">
            {error}
          </div>
        )}

        {/* Step 2 — select tests ------------------------------------------ */}
        {testsReady && (
          <Card>
            <CardTitle
              step="2"
              title="Select test cases to automate"
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
                    onChange={(e) =>
                      setSelected((prev) => {
                        const next = new Set(prev);
                        if (e.target.checked) next.add(tc.id);
                        else next.delete(tc.id);
                        return next;
                      })
                    }
                    className="mt-1 size-4 accent-accent"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-[11px] font-bold text-accent">
                        {tc.id}
                      </span>
                      <span className="text-sm font-medium">{tc.title}</span>
                      <PriorityPill priority={tc.priority} />
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
                        <span className="font-semibold text-ok">Expected:</span>{" "}
                        {tc.expected}
                      </p>
                    )}
                  </div>
                </label>
              ))}

              <div className="mt-2 flex gap-2.5">
                <button
                  onClick={() =>
                    setSelected((prev) =>
                      prev.size === tests.length
                        ? new Set()
                        : new Set(tests.map((t) => t.id)),
                    )
                  }
                  disabled={inputDisabled}
                  className="rounded-lg border border-line px-4 py-2.5 text-sm font-medium transition hover:border-accent/50 disabled:opacity-50"
                >
                  {selected.size === tests.length ? "Deselect All" : "Select All"}
                </button>
                <button
                  onClick={automateSelected}
                  disabled={inputDisabled}
                  className="flex-1 rounded-lg bg-accent px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-accent-deep disabled:opacity-50"
                >
                  {busy && phaseLabel.includes("Phase 2 ·")
                    ? "Automating…"
                    : `Automate Selected (${selected.size})`}
                </button>
              </div>
            </div>
          </Card>
        )}

        {/* Results -------------------------------------------------------- */}
        {frameworkReady && (
          <Card>
            <CardTitle
              step="3"
              title="Generated automation"
              subtitle={`Run ${runId} · output/${runId}/`}
            />
            <div className="px-5 py-4">
              <div className="mb-3.5 flex gap-2">
                {(["pom", "files", "run"] as const).map((t) => (
                  <button
                    key={t}
                    onClick={() => setTab(t)}
                    className={`rounded-md border px-3 py-1.5 text-xs font-medium transition ${
                      tab === t
                        ? "border-accent bg-accent text-white"
                        : "border-line bg-inset text-ink-soft hover:border-accent/40"
                    }`}
                  >
                    {t === "pom"
                      ? "Page Object Model"
                      : t === "files"
                        ? "Framework Files"
                        : "Test Run"}
                  </button>
                ))}
              </div>

              {tab === "pom" && (
                <pre className="max-h-[26rem] overflow-auto rounded-lg border border-line bg-inset p-4 font-mono text-xs leading-relaxed whitespace-pre-wrap">
                  {pomCode.replace(/```(typescript)?/g, "").trim()}
                </pre>
              )}

              {tab === "files" && (
                <div className="flex flex-col gap-2.5">
                  {frameworkFiles.map((f) => (
                    <details
                      key={f.name}
                      className="rounded-lg border border-line bg-inset"
                    >
                      <summary className="cursor-pointer px-3.5 py-2.5 font-mono text-xs font-semibold text-accent">
                        {f.name.replace(`${runId}/`, "")}
                      </summary>
                      <pre className="max-h-72 overflow-auto border-t border-line p-3.5 font-mono text-xs leading-relaxed whitespace-pre-wrap">
                        {f.content}
                      </pre>
                    </details>
                  ))}
                </div>
              )}

              {tab === "run" && (
                <div className="flex flex-col gap-3">
                  <button
                    onClick={runTests}
                    disabled={inputDisabled}
                    className="self-start rounded-lg bg-ok px-5 py-2.5 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
                  >
                    {busy && phaseLabel.includes("Phase 2b")
                      ? "Running…"
                      : "▶ Run Tests"}
                  </button>
                  {runOutput && (
                    <pre
                      className={`max-h-[26rem] overflow-auto rounded-lg border p-4 font-mono text-xs leading-relaxed whitespace-pre-wrap ${
                        runSuccess === true
                          ? "border-ok/30 bg-ok-soft"
                          : runSuccess === false
                            ? "border-err/30 bg-err-soft"
                            : "border-line bg-inset"
                      }`}
                    >
                      {runOutput}
                    </pre>
                  )}
                </div>
              )}
            </div>
          </Card>
        )}
      </main>

      <footer className="py-7 text-center text-[11px] text-ink-soft">
        CrewAI QA Studio · 4 agents on {provider ? `${provider} / ` : ""}
        {model || "an LLM"} · switch provider in .env
      </footer>
    </div>
  );
}
