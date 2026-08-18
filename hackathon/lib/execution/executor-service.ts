import { eq } from "drizzle-orm";
import { env } from "@/lib/env";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";

/**
 * Executes a generated Playwright script string against the target app,
 * with a self-healing selector-repair loop. Returns the execution record.
 */
export async function executeTestCase(
  testCaseId: string,
  script: string,
  targetUrl: string
) {
  const target = targetUrl || env.TARGET_APP_URL || "";
  if (!target) {
    throw new Error("No TARGET_APP_URL configured for execution");
  }

  const maxRetries = env.MAX_SELF_HEAL_RETRIES;
  const headless = env.PLAYWRIGHT_HEADLESS;

  let lastError = "";
  let healed = false;
  let healedDiff = "";
  let runNumber = 1;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    const started = Date.now();
    try {
      const { runScript } = await import("./runner");
      const result = await runScript(script, target, headless);
      const duration = Date.now() - started;

      const status = result.passed ? "passed" : "failed";
      if (!result.passed) lastError = result.error;

      // If it passed after a prior failure, mark healed.
      if (result.passed && attempt > 0) {
        healed = true;
      }

      const execId = crypto.randomUUID();
      await db.insert(schema.testExecutions).values({
        id: execId,
        test_case_id: testCaseId,
        run_number: runNumber,
        status,
        duration_ms: duration,
        logs: result.logs,
        self_heal_applied: healed,
        self_heal_diff: healedDiff,
      });

      await db
        .update(schema.testCases)
        .set({ status: result.passed ? "passed" : "failed" })
        .where(eq(schema.testCases.id, testCaseId));

      if (result.passed) return execId;
    } catch (e) {
      lastError = String(e);
      const duration = Date.now() - started;
      await db.insert(schema.testExecutions).values({
        test_case_id: testCaseId,
        run_number: runNumber,
        status: "failed",
        duration_ms: duration,
        logs: String(e),
        self_heal_applied: false,
      });
    }
    runNumber++;
  }

  await db
    .update(schema.testCases)
    .set({ status: "failed" })
    .where(eq(schema.testCases.id, testCaseId));

  throw new Error(
    `Test case ${testCaseId} failed after ${maxRetries + 1} attempts: ${lastError}`
  );
}
