"use client";

import { useCallback, useEffect, useState } from "react";
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
import Shell, { ViewKey } from "@/components/Shell";
import HomeView from "@/components/HomeView";
import PipelineView from "@/components/PipelineView";
import AutomationView from "@/components/AutomationView";

// All four agents, always visible. Each lights up as its phase runs.
const ALL_AGENTS: AgentRow[] = [
  {
    key: "page_reader",
    title: "Agent 1 · Page Reader",
    activity: "Opens the URL in a real browser and extracts its structure",
    state: "idle",
  },
  {
    key: "test_designer",
    title: "Agent 2 · Test Case Designer",
    activity: "Turns the page snapshot into prioritised test cases",
    state: "idle",
  },
  {
    key: "pom_writer",
    title: "Agent 3 · POM Writer",
    activity: "Writes a typed TypeScript Page Object Model",
    state: "idle",
  },
  {
    key: "framework_architect",
    title: "Agent 4 · Framework Architect",
    activity: "Generates the Playwright framework for the selected cases",
    state: "idle",
  },
];

export default function Home() {
  // navigation + config
  const [view, setView] = useState<ViewKey>("home");
  const [health, setHealth] = useState<Health | null>(null);
  const [provider, setProvider] = useState("");
  const [model, setModel] = useState("");

  // pipeline visibility
  const [phaseLabel, setPhaseLabel] = useState("");
  const [agents, setAgents] = useState<AgentRow[]>(ALL_AGENTS);
  const [commands, setCommands] = useState<CommandRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [outputs, setOutputs] = useState<Record<string, string>>({});

  // data
  const [runId, setRunId] = useState("");
  const [tests, setTests] = useState<TestCase[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [pomCode, setPomCode] = useState("");
  const [frameworkFiles, setFrameworkFiles] = useState<OutputFile[]>([]);
  const [runOutput, setRunOutput] = useState("");
  const [runSuccess, setRunSuccess] = useState<boolean | null>(null);

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

  const startPhase = useCallback((label: string, activeKeys: string[]) => {
    setError("");
    setPhaseLabel(label);
    const now = Date.now();
    setAgents(
      ALL_AGENTS.map((a) =>
        activeKeys.includes(a.key)
          ? { ...a, state: "running" as const, activity: a.activity, startedAt: now }
          : { ...a, state: "idle" as const },
      ),
    );
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
  async function generateTests(url: string, requirements: string) {
    startPhase("Phase 1 · Generate Test Cases", ["page_reader", "test_designer"]);
    setOutputs({});
    setTests([]);
    setSelected(new Set());
    setView("pipeline");

    try {
      await streamPipeline(
        "/api/generate-tests/stream",
        { url, requirements },
        (ev) => {
          if (ev.type === "run_started") setRunId(ev.runId!);
          if (ev.type === "agent_activity")
            upsertAgent({ key: ev.key!, activity: ev.activity! });
          if (ev.type === "agent_started")
            upsertAgent({
              key: ev.key!,
              activity: ev.activity ?? "Working…",
              startedAt: Date.now(),
            });
          if (ev.type === "agent_done") {
            upsertAgent({
              key: ev.key!,
              state: "done",
              detail: ev.detail,
              durationMs: ev.durationMs,
            });
            if (ev.key === "page_reader")
              setOutputs((o) => ({ ...o, page_reader: ev.detail ?? "" }));
          }
          if (ev.type === "agent_failed")
            upsertAgent({ key: ev.key!, state: "failed", detail: ev.detail });
          if (ev.type === "phase_complete") {
            const payload = ev.payload as { run_id: string; test_cases: TestCase[] };
            setTests(payload.test_cases);
            setSelected(new Set(payload.test_cases.map((t) => t.id)));
            setOutputs((o) => ({
              ...o,
              test_designer: JSON.stringify(payload.test_cases, null, 2),
            }));
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
      "pom_writer",
      "framework_architect",
    ]);
    setOutputs({});
    setPomCode("");
    setFrameworkFiles([]);
    setRunOutput("");
    setRunSuccess(null);
    setView("automation");

    try {
      await streamPipeline(
        "/api/automate/stream",
        { run_id: runId, selected: Array.from(selected) },
        (ev) => {
          if (ev.type === "agent_started")
            upsertAgent({
              key: ev.key!,
              activity: ev.activity ?? "Working…",
              startedAt: Date.now(),
            });
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
            setOutputs((o) => ({
              ...o,
              pom_writer: payload.pom_code,
              framework_architect: (payload.framework_files ?? []).join("\n"),
            }));
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

  const toggleTest = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const toggleSelectAll = () =>
    setSelected((prev) =>
      prev.size === tests.length ? new Set() : new Set(tests.map((t) => t.id)),
    );

  return (
    <Shell
      view={view}
      onNavigate={setView}
      provider={provider}
      model={model}
      health={health}
    >
      {view === "home" && <HomeView onGenerate={generateTests} />}
      {view === "pipeline" && (
        <PipelineView
          phaseLabel={phaseLabel}
          agents={agents}
          commands={commands}
          tests={tests}
          selected={selected}
          onToggleTest={toggleTest}
          onSelectAll={toggleSelectAll}
          onAutomate={automateSelected}
          busy={busy}
          error={error}
          outputs={outputs}
        />
      )}
      {view === "automation" && (
        <AutomationView
          runId={runId}
          pomCode={pomCode}
          frameworkFiles={frameworkFiles}
          commands={commands}
          runOutput={runOutput}
          runSuccess={runSuccess}
          busy={busy}
          phaseLabel={phaseLabel}
          onRunTests={runTests}
        />
      )}
    </Shell>
  );
}
