import { NextResponse } from "next/server";
import { eq } from "drizzle-orm";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";

export const runtime = "nodejs";

export async function GET(
  _req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;

  const [run] = await db
    .select()
    .from(schema.pipelineRuns)
    .where(eq(schema.pipelineRuns.id, id))
    .limit(1);

  if (!run) {
    return NextResponse.json({ error: "Run not found" }, { status: 404 });
  }

  const [reqs, plans, cases, executions, defects, evals] = await Promise.all([
    db
      .select()
      .from(schema.requirements)
      .where(eq(schema.requirements.pipeline_run_id, id)),
    db
      .select()
      .from(schema.testPlans)
      .where(eq(schema.testPlans.pipeline_run_id, id)),
    db
      .select()
      .from(schema.testCases)
      .where(eq(schema.testCases.pipeline_run_id, id)),
    db
      .select()
      .from(schema.testExecutions)
      .innerJoin(
        schema.testCases,
        eq(schema.testExecutions.test_case_id, schema.testCases.id)
      )
      .where(eq(schema.testCases.pipeline_run_id, id)),
    db
      .select()
      .from(schema.defects)
      .where(eq(schema.defects.pipeline_run_id, id)),
    db
      .select()
      .from(schema.evalScores)
      .where(eq(schema.evalScores.pipeline_run_id, id)),
  ]);

  return NextResponse.json({
    run,
    requirements: reqs,
    testPlans: plans,
    testCases: cases,
    executions: executions.map(({ test_executions, test_cases }) => ({
      ...test_executions,
      testCaseTitle: test_cases.title,
    })),
    defects,
    evalScores: evals,
  });
}
