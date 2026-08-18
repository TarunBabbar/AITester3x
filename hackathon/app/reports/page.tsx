import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const dynamic = "force-dynamic";

export default async function ReportsPage() {
  const [runs, evals, executions, requirements, testCases, defects] =
    await Promise.all([
      db.select().from(schema.pipelineRuns),
      db.select().from(schema.evalScores),
      db.select().from(schema.testExecutions),
      db.select().from(schema.requirements),
      db.select().from(schema.testCases),
      db.select().from(schema.defects),
    ]);

  const totalReqs = requirements.length;
  const testedReqs = new Set(
    testCases
      .filter((tc) => tc.requirement_id)
      .map((tc) => tc.requirement_id)
  ).size;
  const coveragePct =
    totalReqs > 0 ? Math.round((testedReqs / totalReqs) * 100) : 0;

  const passedExecs = executions.filter((e) => e.status === "passed").length;
  const failedExecs = executions.filter((e) => e.status === "failed").length;
  const healedExecs = executions.filter((e) => e.self_heal_applied).length;

  // Eval trend: average score per run
  const evalByRun = new Map<string, number[]>();
  for (const e of evals) {
    if (!e.pipeline_run_id) continue;
    const arr = evalByRun.get(e.pipeline_run_id) || [];
    arr.push(e.score);
    evalByRun.set(e.pipeline_run_id, arr);
  }
  const runAverages = Array.from(evalByRun.entries()).map(([id, scores]) => ({
    runId: id,
    avg: scores.reduce((a, b) => a + b, 0) / scores.length,
  }));

  const severityCounts = defects.reduce<Record<string, number>>((acc, d) => {
    acc[d.severity || "unknown"] = (acc[d.severity || "unknown"] || 0) + 1;
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Reports & Analysis</h1>
        <p className="text-text-muted mt-1">
          Coverage vs RTM, flaky trends, eval score history.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-text-muted">Requirements</p>
          <p className="text-3xl font-serif font-semibold mt-1">{totalReqs}</p>
        </Card>
        <Card>
          <p className="text-sm text-text-muted">RTM Coverage</p>
          <p className="text-3xl font-serif font-semibold mt-1">{coveragePct}%</p>
          <p className="text-xs text-text-muted mt-1">
            {testedReqs}/{totalReqs} requirements traced
          </p>
        </Card>
        <Card>
          <p className="text-sm text-text-muted">Defects</p>
          <p className="text-3xl font-serif font-semibold mt-1">
            {defects.length}
          </p>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <h2 className="text-lg font-semibold mb-4">Execution Health</h2>
          <div className="flex gap-6">
            <div>
              <p className="text-2xl font-serif font-semibold text-success">
                {passedExecs}
              </p>
              <p className="text-xs text-text-muted">passed</p>
            </div>
            <div>
              <p className="text-2xl font-serif font-semibold text-danger">
                {failedExecs}
              </p>
              <p className="text-xs text-text-muted">failed</p>
            </div>
            <div>
              <p className="text-2xl font-serif font-semibold text-warning">
                {healedExecs}
              </p>
              <p className="text-xs text-text-muted">self-healed</p>
            </div>
          </div>
          {executions.length > 0 && (
            <div className="mt-4">
              <p className="text-xs uppercase text-text-muted mb-1">
                Pass rate
              </p>
              <div className="h-3 rounded-full bg-border overflow-hidden">
                <div
                  className="h-full bg-success rounded-full"
                  style={{
                    width: `${
                      executions.length > 0
                        ? (passedExecs / executions.length) * 100
                        : 0
                    }%`,
                  }}
                />
              </div>
            </div>
          )}
        </Card>

        <Card>
          <h2 className="text-lg font-semibold mb-4">Defects by Severity</h2>
          {Object.keys(severityCounts).length === 0 ? (
            <p className="text-text-muted text-sm">No defect data.</p>
          ) : (
            <div className="space-y-2">
              {Object.entries(severityCounts).map(([sev, count]) => (
                <div key={sev} className="flex items-center justify-between">
                  <span className="text-sm capitalize text-text-muted">
                    {sev}
                  </span>
                  <Badge
                    tone={
                      sev === "critical" || sev === "high" ? "danger" : "warning"
                    }
                  >
                    {count}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <Card>
        <h2 className="text-lg font-semibold mb-4">Eval Score History</h2>
        {runAverages.length === 0 ? (
          <p className="text-text-muted text-sm">
            No eval scores yet — run a pipeline to populate.
          </p>
        ) : (
          <div className="space-y-2">
            {runAverages.map((r) => (
              <div
                key={r.runId}
                className="flex items-center justify-between border border-border rounded-lg p-3"
              >
                <span className="text-xs font-mono text-text-muted">
                  {r.runId.slice(0, 8)}
                </span>
                <div className="flex-1 mx-4">
                  <div className="h-2 rounded-full bg-border overflow-hidden">
                    <div
                      className="h-full bg-accent rounded-full"
                      style={{ width: `${Math.round(r.avg * 100)}%` }}
                    />
                  </div>
                </div>
                <span className="text-sm font-semibold">
                  {Math.round(r.avg * 100)}%
                </span>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <h2 className="text-lg font-semibold mb-4">Runs</h2>
        <div className="space-y-2">
          {runs.map((r) => (
            <div
              key={r.id}
              className="flex items-center justify-between border border-border rounded-lg p-3"
            >
              <div>
                <p className="text-sm font-medium">
                  {r.raw_requirement?.slice(0, 70) || r.id.slice(0, 8)}
                </p>
                <p className="text-xs text-text-muted">
                  {r.created_at
                    ? new Date(r.created_at).toLocaleString()
                    : "—"}{" "}
                  · phase: {r.current_phase || "—"}
                </p>
              </div>
              <Badge
                tone={
                  r.status === "passed"
                    ? "success"
                    : r.status === "running" || r.status === "pending"
                    ? "warning"
                    : "danger"
                }
              >
                {r.status}
              </Badge>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
