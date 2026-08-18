import { env } from "@/lib/env";
import { callWithJsonRepair, jsonOnlyInstruction } from "@/lib/openrouter/json";
import {
  PhaseInput,
  PhaseOutput,
  RequirementOutputSchema,
} from "./types";

const MODEL_KEY = "REQUIREMENT_AGENT_MODEL" as const;

export async function run(input: PhaseInput): Promise<PhaseOutput> {
  const prompt = await getSystemPrompt();
  const messages = [
    { role: "system" as const, content: prompt },
    {
      role: "user" as const,
      content: [
        jsonOnlyInstruction(),
        "Raw requirement text:",
        input.rawRequirement,
      ].join("\n\n"),
    },
  ];

  const model =
    (env as unknown as Record<string, string>)[MODEL_KEY] || undefined;

  const { rawText, output } = await callWithJsonRepair(messages, RequirementOutputSchema, { model });
  return { output, rawText };
}

export async function getSystemPrompt(): Promise<string> {
  // Pull from agent_definitions table if present; fall back to built-in default.
  const { db } = await import("@/lib/db/client");
  const { agentDefinitions } = await import("@/lib/db/schema");
  const { eq } = await import("drizzle-orm");
  const row = await db
    .select()
    .from(agentDefinitions)
    .where(eq(agentDefinitions.name, "requirement-agent"))
    .limit(1);
  return row[0]?.system_prompt || DEFAULT_PROMPT;
}

export const DEFAULT_PROMPT = `You are the Requirement Analysis agent in an AI-driven STLC pipeline.
Analyze the raw requirement text and extract structured requirements.
- Capture every stated requirement exactly as stated; do NOT invent requirements not present in the source text.
- Flag genuinely ambiguous items with is_ambiguous=true and explain the ambiguity. If the source is vague, flag it instead of guessing.
- Assign req_key values like REQ-001.
Respond with JSON only: {"requirements":[{"req_key":"REQ-001","description":"...","acceptance_criteria":"...","is_ambiguous":false,"ambiguity_notes":""}]}`;
