import { NextResponse } from "next/server";
import { eq } from "drizzle-orm";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";

export const runtime = "nodejs";

/**
 * Cancels a running pipeline: marks it failed/stopped. A background run
 * interrupted mid-agent will hit DB write errors on its next step and exit;
 * the cancel flag prevents further phase transitions.
 */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const runId = String(body.runId || "");

  if (!runId) {
    return NextResponse.json({ error: "runId is required" }, { status: 400 });
  }

  const [run] = await db
    .select({ id: schema.pipelineRuns.id, status: schema.pipelineRuns.status })
    .from(schema.pipelineRuns)
    .where(eq(schema.pipelineRuns.id, runId))
    .limit(1);

  if (!run) {
    return NextResponse.json({ error: "Run not found" }, { status: 404 });
  }

  if (run.status !== "running" && run.status !== "pending") {
    return NextResponse.json({
      runId,
      status: run.status,
      note: "Run already finished",
    });
  }

  await db
    .update(schema.pipelineRuns)
    .set({ status: "failed", error: "Cancelled by user" })
    .where(eq(schema.pipelineRuns.id, runId));

  return NextResponse.json({ runId, status: "cancelled" });
}
