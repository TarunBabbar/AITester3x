import { env } from "@/lib/env";
import { callWithJsonRepair, jsonOnlyInstruction } from "@/lib/openrouter/json";
import { PhaseInput, PhaseOutput, DefectOutputSchema } from "./types";

const MODEL_KEY = "TRIAGE_AGENT_MODEL" as const;

export async function run(input: PhaseInput): Promise<PhaseOutput> {
  const prompt = await getSystemPrompt();
  const failures = JSON.stringify(input.context.failedExecutions ?? [], null, 2);

  const messages = [
    { role: "system" as const, content: prompt },
    {
      role: "user" as const,
      content: [
        jsonOnlyInstruction(),
        "Failed test executions:",
        failures,
      ].join("\n\n"),
    },
  ];

  const model =
    (env as unknown as Record<string, string>)[MODEL_KEY] || undefined;

  const { rawText, output } = await callWithJsonRepair(messages, DefectOutputSchema, { model });
  return { output, rawText };
}

export async function getSystemPrompt(): Promise<string> {
  const { db } = await import("@/lib/db/client");
  const { agentDefinitions } = await import("@/lib/db/schema");
  const { eq } = await import("drizzle-orm");
  const row = await db
    .select()
    .from(agentDefinitions)
    .where(eq(agentDefinitions.name, "triage-agent"))
    .limit(1);
  return row[0]?.system_prompt || DEFAULT_PROMPT;
}

export const DEFAULT_PROMPT = `You are the Defect Triage agent in an AI-driven STLC pipeline.
Given failed test execution results, produce structured bug reports.
- Include concrete repro steps and expected vs actual comparison.
- Severity must be plausible and justified by the failure.
- Root-cause hypothesis must be grounded in the cited failure evidence, not fabricated.
- Flag duplicates within the batch via is_duplicate and duplicate_of_title.
Respond with JSON only: {"defects":[{"title":"...","repro_steps":"...","expected":"...","actual":"...","severity":"high","root_cause_hypothesis":"...","is_duplicate":false,"duplicate_of_title":""}]}`;
