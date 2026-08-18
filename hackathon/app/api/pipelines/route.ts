import { NextResponse } from "next/server";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { runPipeline } from "@/lib/agents/orchestrator";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const rawRequirement = String(body.rawRequirement || "").trim();

  if (!rawRequirement) {
    return NextResponse.json(
      { error: "rawRequirement is required" },
      { status: 400 }
    );
  }

  let projectId: string | undefined;
  if (body.projectId) {
    projectId = String(body.projectId);
  } else {
    const [project] = await db
      .insert(schema.projects)
      .values({ name: body.projectName || "Default project" })
      .returning({ id: schema.projects.id });
    projectId = project?.id;
  }

  const runId = crypto.randomUUID();

  // Kick off the pipeline without blocking the HTTP response.
  // The UI polls GET /api/pipelines/[id] for live status.
  void runPipeline(rawRequirement, {
    projectId,
    targetAppUrl: body.targetAppUrl ? String(body.targetAppUrl) : undefined,
    runId,
  }).catch((e) => console.error("Pipeline run crashed:", e));

  return NextResponse.json({ runId, status: "running" }, { status: 202 });
}

export async function GET() {
  const runs = await db
    .select()
    .from(schema.pipelineRuns)
    .orderBy(schema.pipelineRuns.created_at);
  return NextResponse.json({ runs });
}
