import Link from "next/link";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const dynamic = "force-dynamic";

function statusTone(status: string) {
  switch (status) {
    case "passed":
      return "success" as const;
    case "running":
    case "pending":
      return "warning" as const;
    case "failed":
    case "blocked":
      return "danger" as const;
    default:
      return "default" as const;
  }
}

export default async function DashboardPage() {
  const [runs, defects, evalRows, projects] = await Promise.all([
    db
      .select()
      .from(schema.pipelineRuns)
      .orderBy(schema.pipelineRuns.created_at),
    db
      .select()
      .from(schema.defects)
      .orderBy(schema.defects.title)
      .limit(5),
    db.select().from(schema.evalScores).limit(50),
    db.select().from(schema.projects).limit(5),
  ]);

  const passedRuns = runs.filter((r) => r.status === "passed").length;
  const avgEval =
    evalRows.length > 0
      ? evalRows.reduce((a, b) => a + b.score, 0) / evalRows.length
      : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Dashboard</h1>
        <p className="text-text-muted mt-1">
          Active runs, eval score trend, recent defects.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-text-muted">Total Runs</p>
          <p className="text-3xl font-serif font-semibold mt-1">{runs.length}</p>
        </Card>
        <Card>
          <p className="text-sm text-text-muted">Passed Runs</p>
          <p className="text-3xl font-serif font-semibold mt-1">{passedRuns}</p>
        </Card>
        <Card>
          <p className="text-sm text-text-muted">Avg Eval Score</p>
          <p className="text-3xl font-serif font-semibold mt-1">
            {Math.round(avgEval * 100)}%
          </p>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <h2 className="text-lg font-semibold mb-4">Recent Runs</h2>
          {runs.length === 0 ? (
            <p className="text-text-muted text-sm">
              No runs yet.{" "}
              <Link href="/pipelines" className="text-accent underline">
                Create your first pipeline
              </Link>
              .
            </p>
          ) : (
            <ul className="space-y-2">
              {runs.slice(0, 5).map((r) => (
                <li key={r.id}>
                  <Link
                    href={`/pipelines/${r.id}`}
                    className="flex items-center justify-between p-3 rounded-lg border border-border hover:bg-border/30 transition-colors"
                  >
                    <span className="text-sm truncate max-w-[60%]">
                      {r.raw_requirement?.slice(0, 60) || r.id}
                    </span>
                    <Badge tone={statusTone(r.status)}>{r.status}</Badge>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <h2 className="text-lg font-semibold mb-4">Recent Defects</h2>
          {defects.length === 0 ? (
            <p className="text-text-muted text-sm">
              No defects triaged yet.
            </p>
          ) : (
            <ul className="space-y-2">
              {defects.map((d) => (
                <li
                  key={d.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-border"
                >
                  <span className="text-sm truncate max-w-[60%]">{d.title}</span>
                  <Badge tone={statusTone(d.severity || "low")}>
                    {d.severity}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      {projects.length > 0 && (
        <Card>
          <h2 className="text-lg font-semibold mb-4">Projects</h2>
          <ul className="space-y-1">
            {projects.map((p) => (
              <li key={p.id} className="text-sm text-text-muted">
                {p.name}
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
