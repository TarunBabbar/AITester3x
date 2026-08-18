import { NextResponse } from "next/server";
import { eq } from "drizzle-orm";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";

export const runtime = "nodejs";

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const pipelineRunId = searchParams.get("pipelineRunId");

  if (!pipelineRunId) {
    return NextResponse.json(
      { error: "pipelineRunId query param is required" },
      { status: 400 }
    );
  }

  const scores = await db
    .select()
    .from(schema.evalScores)
    .where(eq(schema.evalScores.pipeline_run_id, pipelineRunId))
    .orderBy(schema.evalScores.created_at);

  return NextResponse.json({ scores });
}
