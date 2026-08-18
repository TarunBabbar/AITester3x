"use client";

import { useEffect, useState } from "react";
import { PhaseTimeline } from "@/components/pipeline/PhaseTimeline";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface RunData {
  run: {
    id: string;
    status: string;
    current_phase?: string;
    raw_requirement?: string;
    error?: string;
    created_at?: string;
    completed_at?: string;
  };
  requirements: any[];
  testPlans: any[];
  testCases: any[];
  executions: any[];
  defects: any[];
  evalScores: any[];
}

export default function PipelineDetail({ params }: { params: Promise<{ id: string }> }) {
  const [id, setId] = useState<string>("");
  const [data, setData] = useState<RunData | null>(null);
  const [error, setError] = useState("");
  const [executing, setExecuting] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    params.then((p) => setId(p.id));
  }, [params]);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    async function load() {
      try {
        const res = await fetch(`/api/pipelines/${id}`, { cache: "no-store" });
        if (!res.ok) throw new Error("Failed to load run");
        const json = await res.json();
        if (!cancelled) setData(json);
      } catch (e) {
        if (!cancelled) setError(String(e));
      }
    }
    load();
    const interval = setInterval(load, 4000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [id]);

  async function cancelRun() {
    setCancelling(true);
    try {
      await fetch("/api/pipelines/cancel", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ runId: id }),
      });
    } catch (e) {
      setError(String(e));
    } finally {
      setCancelling(false);
    }
  }

  async function runExecution() {
    setExecuting(true);
    try {
      const res = await fetch("/api/execution/run", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pipelineRunId: id }),
      });
      const json = await res.json();
      if (!res.ok) {
        setError(json.error || "Execution failed");
      }
    } catch (e) {
      setError(String(e));
    } finally {
      setExecuting(false);
    }
  }

  if (!data) {
    return (
      <div>
        <h1 className="text-2xl font-serif font-semibold">Pipeline</h1>
        {error && <p className="text-danger mt-4">{error}</p>}
        {!error && <p className="text-text-muted mt-4">Loading run…</p>}
      </div>
    );
  }

  const { run, requirements, testPlans, testCases, executions, defects, evalScores } =
    data;

  const scoreLookup = evalScores.map((s) => ({
    agent_name: s.agent_name,
    score: s.score,
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-serif font-semibold">Pipeline Detail</h1>
          <p className="text-text-muted mt-1 text-sm">Run {run.id.slice(0, 8)}</p>
        </div>
        <div className="flex items-center gap-3">
          {(run.status === "running" || run.status === "pending") && (
            <Button
              variant="outline"
              onClick={cancelRun}
              disabled={cancelling}
              className="text-danger border-danger/40"
            >
              {cancelling ? "Stopping…" : "Stop"}
            </Button>
          )}
          <Badge tone={run.status === "passed" ? "success" : run.status === "running" || run.status === "pending" ? "warning" : "danger"}>
            {run.status}
          </Badge>
        </div>
      </div>

      {run.error && (
        <div className="bg-danger/10 border border-danger/30 rounded-lg p-4">
          <p className="text-sm font-medium text-danger">Error</p>
          <pre className="whitespace-pre-wrap text-xs text-danger mt-1 font-mono">
            {run.error}
          </pre>
        </div>
      )}

      <Card>
        <PhaseTimeline
          currentPhase={run.current_phase}
          runStatus={run.status}
          scores={scoreLookup}
        />
        {run.status === "running" && (
          <p className="text-sm text-text-muted mt-4 flex items-center gap-2">
            <span className="inline-block w-2 h-2 rounded-full bg-warning animate-pulse" />
            {run.current_phase
              ? `Agent working on ${run.current_phase} phase… this can take a few minutes on free-tier models.`
              : "Starting pipeline…"}
          </p>
        )}
      </Card>

      {run.raw_requirement && (
        <Card>
          <details>
            <summary className="cursor-pointer font-semibold text-lg select-none">
              Requirement
              <span className="ml-2 text-sm font-normal text-text-muted">
                (click to expand)
              </span>
            </summary>
            <pre className="whitespace-pre-wrap text-sm text-text-muted font-sans mt-3 max-h-96 overflow-y-auto">
              {run.raw_requirement}
            </pre>
          </details>
        </Card>
      )}

      {requirements.length > 0 && (
        <Card>
          <h2 className="text-lg font-semibold mb-4">
            Requirements ({requirements.length})
          </h2>
          <div className="space-y-3">
            {requirements.map((r) => (
              <div key={r.id} className="border border-border rounded-lg p-3">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm text-accent">{r.req_key}</span>
                  {r.is_ambiguous && <Badge tone="warning">ambiguous</Badge>}
                </div>
                <p className="text-sm mt-1">{r.description}</p>
                {r.acceptance_criteria && (
                  <p className="text-xs text-text-muted mt-1">
                    AC: {r.acceptance_criteria}
                  </p>
                )}
                {r.ambiguity_notes && (
                  <p className="text-xs text-warning mt-1">{r.ambiguity_notes}</p>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}

      {testPlans.length > 0 && (
        <Card>
          <h2 className="text-lg font-semibold mb-2">Test Plan</h2>
          <p className="text-sm">{testPlans[0].scope}</p>
          {testPlans[0].risk_areas?.length > 0 && (
            <div className="mt-3">
              <p className="text-xs uppercase text-text-muted mb-1">Risk areas</p>
              <div className="flex flex-wrap gap-2">
                {testPlans[0].risk_areas.map((ra: string) => (
                  <Badge key={ra}>{ra}</Badge>
                ))}
              </div>
            </div>
          )}
          {testPlans[0].test_types?.length > 0 && (
            <div className="mt-3">
              <p className="text-xs uppercase text-text-muted mb-1">Test types</p>
              <div className="flex flex-wrap gap-2">
                {testPlans[0].test_types.map((tt: string) => (
                  <Badge key={tt} tone="accent">
                    {tt}
                  </Badge>
                ))}
              </div>
            </div>
          )}
        </Card>
      )}

      {testCases.length > 0 && (
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">
              Test Cases ({testCases.length})
            </h2>
            <Button onClick={runExecution} disabled={executing}>
              {executing ? "Executing…" : "Run Tests"}
            </Button>
          </div>
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {testCases.map((tc) => (
              <details key={tc.id} className="border border-border rounded-lg p-3">
                <summary className="cursor-pointer text-sm font-medium">
                  <span className="mr-2">{tc.title}</span>
                  <Badge tone={tc.case_type === "negative" ? "danger" : tc.case_type === "edge" || tc.case_type === "boundary" ? "warning" : "success"}>
                    {tc.case_type}
                  </Badge>
                  <span className="ml-2 text-xs text-text-muted">{tc.status}</span>
                </summary>
                <pre className="whitespace-pre-wrap text-xs mt-3 bg-white/50 rounded p-3 font-sans">
                  {tc.gherkin}
                </pre>
              </details>
            ))}
          </div>
        </Card>
      )}

      {executions.length > 0 && (
        <Card>
          <h2 className="text-lg font-semibold mb-4">Executions</h2>
          <div className="space-y-2">
            {executions.map((ex) => (
              <div
                key={ex.id}
                className="border border-border rounded-lg p-3 flex items-center justify-between"
              >
                <div>
                  <p className="text-sm font-medium">{ex.testCaseTitle}</p>
                  <p className="text-xs text-text-muted">
                    run #{ex.run_number} · {ex.duration_ms}ms
                    {ex.self_heal_applied && " · self-healed"}
                  </p>
                </div>
                <Badge tone={ex.status === "passed" ? "success" : "danger"}>
                  {ex.status}
                </Badge>
              </div>
            ))}
          </div>
        </Card>
      )}

      {defects.length > 0 && (
        <Card>
          <h2 className="text-lg font-semibold mb-4">
            Defects ({defects.length})
          </h2>
          <div className="space-y-3">
            {defects.map((d) => (
              <div key={d.id} className="border border-border rounded-lg p-3">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium">{d.title}</p>
                  <Badge tone={d.severity === "critical" || d.severity === "high" ? "danger" : "warning"}>
                    {d.severity}
                  </Badge>
                </div>
                {d.root_cause_hypothesis && (
                  <p className="text-xs text-text-muted mt-2">
                    Hypothesis: {d.root_cause_hypothesis}
                  </p>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}

      {evalScores.length > 0 && (
        <Card>
          <h2 className="text-lg font-semibold mb-4">Eval Scores</h2>
          <div className="space-y-2">
            {evalScores.map((s) => (
              <div
                key={s.id}
                className="flex items-center justify-between border border-border rounded-lg p-3"
              >
                <div>
                  <p className="text-sm font-medium">{s.agent_name}</p>
                  <p className="text-xs text-text-muted">
                    {s.metric_name} · threshold {Math.round(s.threshold * 100)}%
                  </p>
                </div>
                <Badge tone={s.passed ? "success" : "danger"}>
                  {Math.round(s.score * 100)}%
                </Badge>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
