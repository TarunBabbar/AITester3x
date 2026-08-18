import { env } from "@/lib/env";
import { callWithJsonRepair, jsonOnlyInstruction } from "@/lib/openrouter/json";
import { PhaseInput, PhaseOutput, ReportOutputSchema } from "./types";

const MODEL_KEY = "REPORTER_AGENT_MODEL" as const;

export async function run(input: PhaseInput): Promise<PhaseOutput> {
  const prompt = await getSystemPrompt();
  const runData = JSON.stringify(input.context, null, 2);

  const messages = [
    { role: "system" as const, content: prompt },
    {
      role: "user" as const,
      content: [
        jsonOnlyInstruction(),
        "Full pipeline run data (requirements, plan, cases, executions, defects, eval scores):",
        runData,
      ].join("\n\n"),
    },
  ];

  const model =
    (env as unknown as Record<string, string>)[MODEL_KEY] || undefined;

  const { rawText, output } = await callWithJsonRepair(messages, ReportOutputSchema, { model });
  return { output, rawText };
}

export async function getSystemPrompt(): Promise<string> {
  const { db } = await import("@/lib/db/client");
  const { agentDefinitions } = await import("@/lib/db/schema");
  const { eq } = await import("drizzle-orm");
  const row = await db
    .select()
    .from(agentDefinitions)
    .where(eq(agentDefinitions.name, "reporter-agent"))
    .limit(1);
  return row[0]?.system_prompt || DEFAULT_PROMPT;
}

export const DEFAULT_PROMPT = `You are the Test Closure / Reporting agent in an AI-driven STLC pipeline.
Given the full run data (requirements, test plan, test cases, executions, defects, eval scores), produce an executive summary.
- Every claim MUST trace back to the provided run data. Never invent numbers, pass rates, or coverage figures.
- Coverage must be computed from the actual requirement and test case data.
- Flaky trends must reference the actual execution history.
Respond with JSON only: {"executive_summary":"...","coverage":{"requirements_tested":0,"total_requirements":0,"percent":0},"risk_gaps":["..."],"flaky_trends":["..."],"recommendations":["..."]}`;
