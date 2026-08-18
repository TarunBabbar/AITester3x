import { eq } from "drizzle-orm";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const dynamic = "force-dynamic";

export default async function ExecutionPage() {
  const executions = await db
    .select({
      id: schema.testExecutions.id,
      run_number: schema.testExecutions.run_number,
      status: schema.testExecutions.status,
      duration_ms: schema.testExecutions.duration_ms,
      logs: schema.testExecutions.logs,
      self_heal_applied: schema.testExecutions.self_heal_applied,
      self_heal_diff: schema.testExecutions.self_heal_diff,
      executed_at: schema.testExecutions.executed_at,
      title: schema.testCases.title,
    })
    .from(schema.testExecutions)
    .innerJoin(
      schema.testCases,
      eq(schema.testExecutions.test_case_id, schema.testCases.id)
    )
    .orderBy(schema.testExecutions.executed_at);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Test Execution</h1>
        <p className="text-text-muted mt-1">
          Execution results, screenshots, and self-heal logs.
        </p>
      </div>

      <Card>
        {executions.length === 0 ? (
          <p className="text-text-muted text-sm">
            No executions yet. Run a pipeline, then click “Run Tests” on the
            pipeline detail page.
          </p>
        ) : (
          <div className="space-y-3">
            {executions.map((ex) => (
              <div key={ex.id} className="border border-border rounded-lg p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium text-sm">{ex.title}</p>
                    <p className="text-xs text-text-muted mt-0.5">
                      run #{ex.run_number} · {ex.duration_ms}ms ·{" "}
                      {new Date(ex.executed_at || "").toLocaleString()}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {ex.self_heal_applied && <Badge tone="warning">self-healed</Badge>}
                    <Badge tone={ex.status === "passed" ? "success" : "danger"}>
                      {ex.status}
                    </Badge>
                  </div>
                </div>
                {ex.logs && (
                  <pre className="whitespace-pre-wrap text-xs mt-3 bg-white/50 rounded p-3 max-h-40 overflow-y-auto font-mono">
                    {ex.logs}
                  </pre>
                )}
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
