import { env } from "@/lib/env";
import { callWithJsonRepair, jsonOnlyInstruction } from "@/lib/openrouter/json";
import { PhaseInput, PhaseOutput, TestPlanOutputSchema } from "./types";

const MODEL_KEY = "PLANNER_AGENT_MODEL" as const;

export async function run(input: PhaseInput): Promise<PhaseOutput> {
  const prompt = await getSystemPrompt();
  const requirements = JSON.stringify(input.context.requirements ?? [], null, 2);

  const messages = [
    { role: "system" as const, content: prompt },
    {
      role: "user" as const,
      content: [
        jsonOnlyInstruction(),
        "Structured requirements:",
        requirements,
      ].join("\n\n"),
    },
  ];

  const model =
    (env as unknown as Record<string, string>)[MODEL_KEY] || undefined;

  const { rawText, output } = await callWithJsonRepair(messages, TestPlanOutputSchema, { model });
  return { output, rawText };
}

export async function getSystemPrompt(): Promise<string> {
  const { db } = await import("@/lib/db/client");
  const { agentDefinitions } = await import("@/lib/db/schema");
  const { eq } = await import("drizzle-orm");
  const row = await db
    .select()
    .from(agentDefinitions)
    .where(eq(agentDefinitions.name, "planner-agent"))
    .limit(1);
  return row[0]?.system_prompt || DEFAULT_PROMPT;
}

export const DEFAULT_PROMPT = `You are the Test Planning agent in an AI-driven STLC pipeline.
Given structured requirements, produce a test plan.
- Scope must be grounded in the actual requirement content, not generic boilerplate.
- Risk areas must reference real aspects of the requirements.
- Test types must be justified by the requirements (functional, regression, api, ui, security, performance as appropriate).
Respond with JSON only: {"scope":"...","risk_areas":["..."],"test_types":["..."]}`;
