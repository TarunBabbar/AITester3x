import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const dynamic = "force-dynamic";

function severityTone(severity: string) {
  switch (severity) {
    case "critical":
    case "high":
      return "danger" as const;
    case "medium":
      return "warning" as const;
    default:
      return "default" as const;
  }
}

export default async function DefectsPage() {
  const defects = await db
    .select()
    .from(schema.defects)
    .orderBy(schema.defects.title);

  const severityCounts = defects.reduce<Record<string, number>>((acc, d) => {
    acc[d.severity || "unknown"] = (acc[d.severity || "unknown"] || 0) + 1;
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Defects</h1>
        <p className="text-text-muted mt-1">
          Triaged bugs, duplicates, and severity.
        </p>
      </div>

      {Object.keys(severityCounts).length > 0 && (
        <div className="flex flex-wrap gap-3">
          {Object.entries(severityCounts).map(([sev, count]) => (
            <Card key={sev} className="p-4">
              <p className="text-sm text-text-muted capitalize">{sev}</p>
              <p className="text-2xl font-serif font-semibold">{count}</p>
            </Card>
          ))}
        </div>
      )}

      <Card>
        {defects.length === 0 ? (
          <p className="text-text-muted text-sm">
            No defects triaged yet.
          </p>
        ) : (
          <div className="space-y-3">
            {defects.map((d) => (
              <div key={d.id} className="border border-border rounded-lg p-4">
                <div className="flex items-center justify-between">
                  <p className="font-medium">{d.title}</p>
                  <Badge tone={severityTone(d.severity || "")}>
                    {d.severity}
                  </Badge>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3 text-sm">
                  <div>
                    <p className="text-xs uppercase text-text-muted mb-1">Expected</p>
                    <p className="text-text-muted">{d.expected || "—"}</p>
                  </div>
                  <div>
                    <p className="text-xs uppercase text-text-muted mb-1">Actual</p>
                    <p className="text-text-muted">{d.actual || "—"}</p>
                  </div>
                </div>
                {d.repro_steps && (
                  <div className="mt-3 text-sm">
                    <p className="text-xs uppercase text-text-muted mb-1">
                      Repro steps
                    </p>
                    <pre className="whitespace-pre-wrap text-xs bg-white/50 rounded p-3 font-mono">
                      {d.repro_steps}
                    </pre>
                  </div>
                )}
                {d.root_cause_hypothesis && (
                  <p className="text-xs text-text-muted mt-3">
                    Root cause hypothesis: {d.root_cause_hypothesis}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
