"use client";

import { useCallback, useEffect, useState } from "react";
import {
  AgentRow,
  CommandRow,
  EvalRow,
  getConfig,
  getHealth,
  Health,
  listOutputFiles,
  OutputFile,
  stopPipeline,
  streamPipeline,
  TestCase,
} from "@/lib/api";
import Shell, { ViewKey } from "@/components/Shell";
import HomeView from "@/components/HomeView";
import PipelineView from "@/components/PipelineView";
import AutomationView from "@/components/AutomationView";
import RunView from "@/components/RunView";

// All five agents, always visible. Each lights up as its phase runs.
const ALL_AGENTS: AgentRow[] = [
  {
    key: "ri",
    title: "Requirement Intelligence",
    activity: "Converts the URL + requirements into structured intelligence",
    state: "idle",
  },
  {
    key: "page_reader",
    title: "Page Reader",
    activity: "Opens the URL in a real browser and extracts its structure",
    state: "idle",
  },
  {
    key: "test_designer",
    title: "Test Case Designer",
    activity: "Turns the RI into prioritised test cases",
    state: "idle",
  },
  {
    key: "pom_writer",
    title: "POM Writer",
    activity: "Writes a typed TypeScript Page Object Model",
    state: "idle",
  },
  {
    key: "framework_architect",
    title: "Framework Architect",
    activity: "Generates the Playwright framework for the selected cases",
    state: "idle",
  },
];

// Which agents belong to which view (order matters — run sequence)
const PIPELINE_AGENT_KEYS = ["ri", "page_reader", "test_designer"];
const AUTOMATION_AGENT_KEYS = ["pom_writer", "framework_architect"];
export default function Home() {
  // navigation + config
  const [view, setView] = useState<ViewKey>("home");
  const [health, setHealth] = useState<Health | null>(null);
  const [provider, setProvider] = useState("");
  const [model, setModel] = useState("");

  // pipeline visibility
  const [phaseLabel, setPhaseLabel] = useState("");
  const [agents, setAgents] = useState<AgentRow[]>(ALL_AGENTS);
  const [evals, setEvals] = useState<EvalRow[]>([]);
  const [commands, setCommands] = useState<CommandRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [outputs, setOutputs] = useState<Record<string, string>>({});
  const [docker, setDocker] = useState<{ available: boolean } | null>(null);
  const [releaseScore, setReleaseScore] = useState<number | null>(null);

  // data
  const [runId, setRunId] = useState("");
  const [riText, setRiText] = useState("");
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
    getConfig()
      .then((c) => {
        setProvider(c.provider);
        setModel(c.model);
      })
      .catch(() => {});
  }, []);

  // Live Docker check — re-hits the backend health endpoint
  const checkDocker = useCallback(() => {
    getHealth()
      .then((h) => {
        setHealth(h);
        if (h.docker) setDocker(h.docker);
      })
      .catch(() => setHealth(null));
  }, []);

  // Check on mount and poll every 8s so the Run view stays accurate
  useEffect(() => {
    checkDocker();
    const id = setInterval(checkDocker, 8000);
    return () => clearInterval(id);
  }, [checkDocker]);

  // On mount, load the latest generated run's files (so Test Automation and
  // Test Run show existing output even after a refresh or direct navigation)
  useEffect(() => {
    listOutputFiles()
      .then((files) => {
        if (files.length === 0) return;
        // Files are named "<run_id>/path" — group by run id, pick the newest
        const runIds = Array.from(
          new Set(files.map((f) => f.name.split("/")[0])),
        );
        const latest = runIds.sort().reverse()[0];
        if (!latest) return;
        setRunId(latest);
        setFrameworkFiles(files.filter((f) => f.name.startsWith(`${latest}/`)));
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

  const upsertEval = useCallback(
    (patch: Partial<EvalRow> & { key: string }) => {
      setEvals((rows) => {
        const idx = rows.findIndex((r) => r.key === patch.key);
        if (idx === -1) return [...rows, patch as EvalRow];
        const next = [...rows];
        next[idx] = { ...next[idx], ...patch } as EvalRow;
        return next;
      });
    },
    [],
  );

  const startPhase = useCallback((label: string, activeKeys: string[]) => {
    setError("");
    setPhaseLabel(label);
    // All active agents start queued. The backend's agent_started events
    // flip each one to "running" in the real execution order — the UI never
    // guesses which agent is running.
    setAgents(
      ALL_AGENTS.map((a) =>
        activeKeys.includes(a.key)
          ? { ...a, state: "queued" as const, activity: "Waiting to start…" }
          : { ...a, state: "idle" as const },
      ),
    );
    setCommands([]);
    setEvals([]);
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

  // Stop the currently running pipeline on the server side
  const stopRun = useCallback(() => {
    if (!runId) return;
    setPhaseLabel("Stopping…");
    stopPipeline(runId).catch(() => {});
  }, [runId]);

  const handleRunStopped = useCallback(() => {
    setPhaseLabel("Run stopped");
    setAgents((rows) =>
      rows.map((r) =>
        r.state === "running"
          ? { ...r, state: "idle", activity: "Stopped by user" }
          : r,
      ),
    );
  }, []);

  // -------------------------------------------------------------------------
  // Phase 1 — generate test cases
  // -------------------------------------------------------------------------
  async function generateTests(url: string, requirements: string) {
    startPhase("Phase 1 · Generate Test Cases", ["ri", "page_reader", "test_designer"]);
    setOutputs({});
    setRiText("");
    setTests([]);
    setSelected(new Set());
    setView("pipeline");

    try {
      await streamPipeline(
        "/api/generate-tests/stream",
        { url, requirements },
        (ev) => {
          if (ev.type === "run_started") setRunId(ev.runId!);
          if (ev.type === "agent_started")
            upsertAgent({
              key: ev.key!,
              state: "running",
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
            if (ev.key === "ri" && ev.output)
              setRiText(ev.output);
          }
          if (ev.type === "agent_failed")
            upsertAgent({ key: ev.key!, state: "failed", detail: ev.detail });
          if (ev.type === "eval_started")
            upsertEval({
              key: ev.key!,
              title: ev.title ?? "Evaluation",
              activity: ev.activity ?? "Running…",
              state: "running",
            });
          if (ev.type === "eval_done")
            upsertEval({
              key: ev.key!,
              state: "done",
              result: ev.result,
              activity: ev.result?.reason ?? "Done",
            });
          if (ev.type === "eval_failed")
            upsertEval({
              key: ev.key!,
              state: "failed",
              activity: ev.detail ?? "Evaluation failed",
            });
          if (ev.type === "phase_complete") {
            const payload = ev.payload as {
              run_id: string;
              test_cases: TestCase[];
              ri_text?: string;
            };
            setTests(payload.test_cases);
            setSelected(new Set(payload.test_cases.map((t) => t.id)));
            if (payload.ri_text) setRiText(payload.ri_text);
            setOutputs((o) => ({
              ...o,
              test_designer: JSON.stringify(payload.test_cases, null, 2),
            }));
          }
          if (ev.type === "run_stopped") handleRunStopped();
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
              state: "running",
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
          }
          if (ev.type === "agent_failed")
            upsertAgent({ key: ev.key!, state: "failed", detail: ev.detail });
          if (ev.type === "eval_started")
            upsertEval({
              key: ev.key!,
              title: ev.title ?? "Evaluation",
              activity: ev.activity ?? "Running…",
              state: "running",
            });
          if (ev.type === "eval_done")
            upsertEval({
              key: ev.key!,
              state: "done",
              result: ev.result,
              activity: ev.result?.reason ?? "Done",
            });
          if (ev.type === "eval_failed")
            upsertEval({
              key: ev.key!,
              state: "failed",
              activity: ev.detail ?? "Evaluation failed",
            });
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
          if (ev.type === "run_stopped") handleRunStopped();
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
  // Phase 2b — run the generated tests (optionally a selected subset)
  // -------------------------------------------------------------------------
  async function runTests(testNames: string[]) {
    // Fresh Docker check before starting the run
    checkDocker();
    startPhase("Phase 2b · Run Generated Tests", []);
    setRunSuccess(null);
    setRunOutput("");
    setReleaseScore(null);
    try {
      await streamPipeline(
        "/api/run-tests/stream",
        { run_id: runId, test_names: testNames },
        (ev) => {
          if (ev.type === "docker_check")
            setDocker({ available: false });
          if (ev.type === "docker_ok")
            setDocker({ available: true });
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
          if (ev.type === "release_score")
            setReleaseScore(ev.score ?? null);
          if (ev.type === "phase_complete") {
            const payload = ev.payload as {
              success: boolean;
              output: string;
              release_score?: number;
            };
            setRunSuccess(payload.success);
            setRunOutput(payload.output);
            if (payload.release_score !== undefined)
              setReleaseScore(payload.release_score);
          }
          if (ev.type === "run_stopped") handleRunStopped();
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
          agents={agents.filter((a) => PIPELINE_AGENT_KEYS.includes(a.key))}
          evals={evals}
          commands={commands}
          tests={tests}
          selected={selected}
          riText={riText}
          onToggleTest={toggleTest}
          onSelectAll={toggleSelectAll}
          onAutomate={automateSelected}
          onStop={stopRun}
          busy={busy}
          error={error}
          outputs={outputs}
        />
      )}
      {view === "automation" && (
        <AutomationView
          runId={runId}
          frameworkFiles={frameworkFiles}
          agents={agents.filter((a) => AUTOMATION_AGENT_KEYS.includes(a.key))}
          evals={evals.filter((e) => e.key === "automation_eval")}
        />
      )}
      {view === "run" && (
        <RunView
          runId={runId}
          frameworkFiles={frameworkFiles}
          evals={evals}
          commands={commands}
          runOutput={runOutput}
          runSuccess={runSuccess}
          releaseScore={releaseScore}
          docker={docker}
          busy={busy}
          phaseLabel={phaseLabel}
          onRunTests={runTests}
          onStop={stopRun}
        />
      )}
    </Shell>
  );
}
