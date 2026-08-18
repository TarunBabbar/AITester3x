import Link from "next/link";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import NewPipelineForm from "@/components/pipeline/NewPipelineForm";

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

export default async function PipelinesPage() {
  const runs = await db
    .select()
    .from(schema.pipelineRuns)
    .orderBy(schema.pipelineRuns.created_at);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Pipelines</h1>
        <p className="text-text-muted mt-1">
          All pipeline runs across the AI STLC workflow.
        </p>
      </div>

      <NewPipelineForm />

      <Card>
        <h2 className="text-lg font-semibold mb-4">Run History</h2>
        {runs.length === 0 ? (
          <p className="text-text-muted text-sm">No runs yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-text-muted border-b border-border">
                  <th className="py-2 pr-4">Requirement</th>
                  <th className="py-2 pr-4">Phase</th>
                  <th className="py-2 pr-4">Status</th>
                  <th className="py-2">Created</th>
                </tr>
              </thead>
              <tbody>
                {runs.map((r) => (
                  <tr key={r.id} className="border-b border-border/50">
                    <td className="py-2 pr-4">
                      <Link
                        href={`/pipelines/${r.id}`}
                        className="text-accent hover:underline"
                      >
                        {r.raw_requirement?.slice(0, 60) || r.id.slice(0, 8)}
                      </Link>
                    </td>
                    <td className="py-2 pr-4 capitalize">{r.current_phase || "—"}</td>
                    <td className="py-2 pr-4">
                      <Badge tone={statusTone(r.status)}>{r.status}</Badge>
                    </td>
                    <td className="py-2 text-text-muted">
                      {r.created_at
                        ? new Date(r.created_at).toLocaleString()
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
