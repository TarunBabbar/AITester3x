import { env } from "@/lib/env";
import { callWithJsonRepair, jsonOnlyInstruction } from "@/lib/openrouter/json";
import { PhaseInput, PhaseOutput, ExecutionOutputSchema } from "./types";

const MODEL_KEY = "EXECUTOR_AGENT_MODEL" as const;

export async function run(input: PhaseInput): Promise<PhaseOutput> {
  const prompt = await getSystemPrompt();
  const testCases = JSON.stringify(input.context.testCases ?? [], null, 2);

  const messages = [
    { role: "system" as const, content: prompt },
    {
      role: "user" as const,
      content: [
        jsonOnlyInstruction(),
        `Target app URL: ${input.targetAppUrl || env.TARGET_APP_URL || "NOT SET"}`,
        "Test cases to script:",
        testCases,
      ].join("\n\n"),
    },
  ];

  const model =
    (env as unknown as Record<string, string>)[MODEL_KEY] || undefined;

  const { rawText, output } = await callWithJsonRepair(messages, ExecutionOutputSchema, { model });
  return { output, rawText };
}

export async function getSystemPrompt(): Promise<string> {
  const { db } = await import("@/lib/db/client");
  const { agentDefinitions } = await import("@/lib/db/schema");
  const { eq } = await import("drizzle-orm");
  const row = await db
    .select()
    .from(agentDefinitions)
    .where(eq(agentDefinitions.name, "executor-agent"))
    .limit(1);
  return row[0]?.system_prompt || DEFAULT_PROMPT;
}

export const DEFAULT_PROMPT = `You are the Test Execution agent in an AI-driven STLC pipeline.
You write Playwright (playwright-core) scripts in plain JavaScript (CommonJS, no imports beyond the skeleton below) that run against a target web app.
- Use robust, resilient locators: prefer roles, text, placeholders, and data-testid attributes over brittle CSS.
- Script must: launch chromium, goto the target URL, perform the scenario steps, take a screenshot, close the browser.
- The target URL will be injected as a global variable named TARGET_URL.
- Use this exact skeleton:
const { chromium } = require('playwright-core');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  try {
    await page.goto(TARGET_URL, { waitUntil: 'domcontentloaded', timeout: ${env.TEST_EXECUTION_TIMEOUT_MS} });
    // ... scenario steps ...
    await page.screenshot({ path: 'screenshot.png' });
    console.log('TEST_PASS');
  } catch (e) {
    console.error('TEST_FAIL: ' + (e.message || e));
    process.exit(1);
  } finally {
    await browser.close();
  }
})();
Respond with JSON only: {"scripts":[{"test_case_id":"...","script":"..."}]}`;
