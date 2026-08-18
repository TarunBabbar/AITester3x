import { NextResponse } from "next/server";
import { eq } from "drizzle-orm";
import { env } from "@/lib/env";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { executeTestCase } from "@/lib/execution/executor-service";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const pipelineRunId = String(body.pipelineRunId || "");
  if (!pipelineRunId) {
    return NextResponse.json(
      { error: "pipelineRunId is required" },
      { status: 400 }
    );
  }

  const target = String(body.targetAppUrl || env.TARGET_APP_URL || "");
  if (!target) {
    return NextResponse.json(
      { error: "TARGET_APP_URL not configured" },
      { status: 400 }
    );
  }

  const cases = await db
    .select()
    .from(schema.testCases)
    .where(eq(schema.testCases.pipeline_run_id, pipelineRunId));

  if (cases.length === 0) {
    return NextResponse.json(
      { error: "No test cases to execute — run testcase phase first" },
      { status: 400 }
    );
  }

  const results: { testCaseId: string; ok: boolean; error?: string }[] = [];

  for (const tc of cases) {
    try {
      const execId = await executeTestCase(tc.id, tc.generated_script || "", target);
      results.push({ testCaseId: tc.id, ok: true });
      void execId;
    } catch (e) {
      results.push({ testCaseId: tc.id, ok: false, error: String(e) });
    }
  }

  return NextResponse.json({ results });
}
