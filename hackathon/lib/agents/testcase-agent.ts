import { env } from "@/lib/env";
import { callWithJsonRepair, jsonOnlyInstruction } from "@/lib/openrouter/json";
import { PhaseInput, PhaseOutput, TestCaseOutputSchema } from "./types";

const MODEL_KEY = "TESTCASE_AGENT_MODEL" as const;

export async function run(input: PhaseInput): Promise<PhaseOutput> {
  const prompt = await getSystemPrompt();
  const requirements = JSON.stringify(input.context.requirements ?? [], null, 2);
  const plan = JSON.stringify(input.context.testPlan ?? {}, null, 2);

  const messages = [
    { role: "system" as const, content: prompt },
    {
      role: "user" as const,
      content: [
        jsonOnlyInstruction(),
        "Structured requirements:",
        requirements,
        "Test plan:",
        plan,
      ].join("\n\n"),
    },
  ];

  const model =
    (env as unknown as Record<string, string>)[MODEL_KEY] || undefined;

  const { rawText, output } = await callWithJsonRepair(messages, TestCaseOutputSchema, { model });
  return { output, rawText };
}

export async function getSystemPrompt(): Promise<string> {
  const { db } = await import("@/lib/db/client");
  const { agentDefinitions } = await import("@/lib/db/schema");
  const { eq } = await import("drizzle-orm");
  const row = await db
    .select()
    .from(agentDefinitions)
    .where(eq(agentDefinitions.name, "testcase-agent"))
    .limit(1);
  return row[0]?.system_prompt || DEFAULT_PROMPT;
}

export const DEFAULT_PROMPT = `You are the Test Case Design agent in an AI-driven STLC pipeline.
Given requirements and a test plan, design structured Gherkin-style test cases.
- Every test case MUST trace to a real requirement via req_key (use only req_keys present in the requirements list).
- Cover positive, negative, edge, and boundary cases — not just happy path.
- Each test case needs a title, Gherkin scenario, and case_type.
Respond with JSON only: {"test_cases":[{"req_key":"REQ-001","title":"...","gherkin":"Feature: ...\\nScenario: ...\\n  Given ...\\n  When ...\\n  Then ...","case_type":"positive"}]}`;
