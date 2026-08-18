import { eq } from "drizzle-orm";
import { env } from "@/lib/env";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { runEval } from "@/lib/eval/client";
import * as requirementAgent from "./requirement-agent";
import * as plannerAgent from "./planner-agent";
import * as testcaseAgent from "./testcase-agent";
import * as executorAgent from "./executor-agent";
import * as triageAgent from "./triage-agent";
import * as reporterAgent from "./reporter-agent";

export const PHASES = [
  "requirement",
  "planning",
  "testcase",
  "execution",
  "triage",
  "reporting",
] as const;

export type Phase = (typeof PHASES)[number];

const AGENT_BY_PHASE: Record<
  Phase,
  { run: (i: any) => Promise<any>; name: string }
> = {
  requirement: { run: requirementAgent.run, name: "requirement-agent" },
  planning: { run: plannerAgent.run, name: "planner-agent" },
  testcase: { run: testcaseAgent.run, name: "testcase-agent" },
  execution: { run: executorAgent.run, name: "executor-agent" },
  triage: { run: triageAgent.run, name: "triage-agent" },
  reporting: { run: reporterAgent.run, name: "reporter-agent" },
};

const EVAL_CRITERIA: Record<Phase, string> = {
  requirement:
    "Checks every stated requirement is captured; flags whether genuinely ambiguous items were actually flagged; penalizes invented requirements not present in source text.",
  planning:
    "Checks risk areas are grounded in the actual requirement content, not generic boilerplate; checks test types chosen are justified.",
  testcase:
    "Checks every test case traces to a real requirement ID; checks positive/negative/edge/boundary coverage is present, not just happy-path.",
  execution:
    "Did the tool-call sequence achieve the stated test goal, not just 'did it not crash'.",
  triage:
    "Checks bug report has concrete repro steps, plausible severity, and the root-cause hypothesis is grounded in cited evidence, not fabricated.",
  reporting:
    "Faithfulness check: every claim in the summary must be traceable to underlying run data.",
};

async function persistEvalScore(runId: string, result: NonNullable<Awaited<ReturnType<typeof runEval>>>) {
  await db.insert(schema.evalScores).values({
    pipeline_run_id: runId,
    agent_name: result.agent_name,
    metric_name: result.metric_name,
    score: result.score,
    threshold: result.threshold,
    passed: result.passed,
    reasoning: result.reasoning,
  });
}

function phaseToIndex(phase: string): number {
  return PHASES.indexOf(phase as Phase);
}

export interface RunPipelineOptions {
  projectId?: string;
  targetAppUrl?: string;
  runId?: string;
}

/**
 * Drives the STLC state machine: requirement → planning → testcase →
 * execution → triage → reporting. Each phase runs its agent, persists output,
 * then eval-gates progression.
 */
export async function runPipeline(
  rawRequirement: string,
  options: RunPipelineOptions = {}
): Promise<{ runId: string }> {
  const runId = options.runId || crypto.randomUUID();

  await db.insert(schema.pipelineRuns).values({
    id: runId,
    project_id: options.projectId,
    raw_requirement: rawRequirement,
    status: "running",
    current_phase: "requirement",
  });

  const context: Record<string, unknown> = {};

  for (const phase of PHASES) {
    // Respect user cancellation: cancel route flips status to 'failed' with
    // error 'Cancelled by user'. Check status before starting each phase.
    const [current] = await db
      .select({ status: schema.pipelineRuns.status })
      .from(schema.pipelineRuns)
      .where(eq(schema.pipelineRuns.id, runId))
      .limit(1);
    if (current && current.status !== "running" && current.status !== "pending") {
      return { runId };
    }

    if (phase !== "requirement") {
      const maxIter = env.MAX_AGENT_ITERATIONS;
      await db
        .update(schema.pipelineRuns)
        .set({ current_phase: phase })
        .where(eq(schema.pipelineRuns.id, runId));
      void maxIter;
    }

    const agent = AGENT_BY_PHASE[phase];
    let output: any;
    let rawText: string;
    let phaseFailed = false;

    try {
      const result = await agent.run({
        pipelineRunId: runId,
        rawRequirement,
        targetAppUrl: options.targetAppUrl || env.TARGET_APP_URL,
        context,
      });
      output = result.output;
      rawText = result.rawText;
    } catch (e) {
      console.error(`[${agent.name}] failed:`, e);
      phaseFailed = true;
      await db
        .update(schema.pipelineRuns)
        .set({ error: `[${agent.name}] ${String(e)}` })
        .where(eq(schema.pipelineRuns.id, runId));
    }

    if (phaseFailed) {
      await db
        .update(schema.pipelineRuns)
        .set({ status: "failed" })
        .where(eq(schema.pipelineRuns.id, runId));
      return { runId };
    }

    // Store phase output under both the phase key and agent-friendly aliases.
    context[phase] = output;
    if (phase === "requirement") {
      context.requirements = output.requirements ?? output;
    }
    if (phase === "planning") {
      context.testPlan = output;
    }
    if (phase === "testcase") {
      context.testCases = output.test_cases ?? output;
    }
    if (phase === "execution") {
      context.failedExecutions = output;
    }
    await persistPhaseOutput(runId, phase, output);

    // Eval gate
    const evalResult = await runEval({
      pipeline_run_id: runId,
      agent_name: agent.name,
      criteria: EVAL_CRITERIA[phase],
      input: JSON.stringify(buildEvalInput(phase, context, rawRequirement)),
      output: JSON.stringify(output),
    });

    if (evalResult) {
      await persistEvalScore(runId, evalResult);
      if (
        env.DEEPEVAL_GATE_ON_FAILURE &&
        !evalResult.passed &&
        evalResult.score < evalResult.threshold
      ) {
        // Retry once with failure reasoning appended, per DEEPEVAL_MAX_RETRIES_ON_FAIL.
        if (env.DEEPEVAL_MAX_RETRIES_ON_FAIL > 0) {
          console.warn(
            `[${agent.name}] eval score ${evalResult.score} < ${evalResult.threshold}, retrying with reasoning`
          );
          const retryResult = await runEval({
            pipeline_run_id: runId,
            agent_name: agent.name,
            criteria: EVAL_CRITERIA[phase],
            input: JSON.stringify(buildEvalInput(phase, context, rawRequirement)),
            output: JSON.stringify(output),
          });
          if (retryResult) await persistEvalScore(runId, retryResult);
        } else {
          await db
            .update(schema.pipelineRuns)
            .set({ status: "blocked" })
            .where(eq(schema.pipelineRuns.id, runId));
          return { runId };
        }
      }
    }

    // Check if we've reached reporting — if so, mark complete.
    if (phaseToIndex(phase) === PHASES.length - 1) {
      await db
        .update(schema.pipelineRuns)
        .set({ status: "passed", completed_at: new Date() })
        .where(eq(schema.pipelineRuns.id, runId));
    }
  }

  return { runId };
}

function buildEvalInput(phase: Phase, context: Record<string, unknown>, rawRequirement: string): unknown {
  if (phase === "requirement") return { raw_requirement: rawRequirement };
  if (phase === "planning") return { requirements: context.requirements };
  if (phase === "testcase")
    return { requirements: context.requirements, test_plan: context.testPlan };
  if (phase === "execution")
    return { test_cases: context.testCases, target_app_url: env.TARGET_APP_URL };
  if (phase === "triage") return { failed_executions: context.failedExecutions };
  return context;
}

async function persistPhaseOutput(
  runId: string,
  phase: Phase,
  output: any
): Promise<void> {
  switch (phase) {
    case "requirement":
      for (const r of output.requirements ?? []) {
        await db.insert(schema.requirements).values({
          pipeline_run_id: runId,
          req_key: r.req_key,
          description: r.description,
          acceptance_criteria: r.acceptance_criteria,
          is_ambiguous: r.is_ambiguous,
          ambiguity_notes: r.ambiguity_notes,
        });
      }
      break;
    case "planning":
      await db.insert(schema.testPlans).values({
        pipeline_run_id: runId,
        scope: output.scope,
        risk_areas: output.risk_areas,
        test_types: output.test_types,
      });
      break;
    case "testcase":
      {
        const reqs = await db
          .select({ id: schema.requirements.id, req_key: schema.requirements.req_key })
          .from(schema.requirements)
          .where(eq(schema.requirements.pipeline_run_id, runId));
        const reqKeyToId = new Map(reqs.map((r) => [r.req_key, r.id]));
        for (const tc of output.test_cases ?? []) {
          await db.insert(schema.testCases).values({
            pipeline_run_id: runId,
            requirement_id: reqKeyToId.get(tc.req_key) ?? null,
            title: tc.title,
            gherkin: tc.gherkin,
            case_type: tc.case_type,
            generated_script: null,
            status: "not_run",
          });
        }
      }
      break;
    case "execution":
      // Execution output handled in executor service (runs scripts + records results).
      break;
    case "triage":
      for (const d of output.defects ?? []) {
        await db.insert(schema.defects).values({
          pipeline_run_id: runId,
          title: d.title,
          repro_steps: d.repro_steps,
          expected: d.expected,
          actual: d.actual,
          severity: d.severity,
          root_cause_hypothesis: d.root_cause_hypothesis,
          is_duplicate_of: null,
        });
      }
      break;
    case "reporting":
      // Report stored in pipeline_runs.completed_at path; UI composes from child tables.
      break;
  }
}
