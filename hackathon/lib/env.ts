import { z } from "zod";

const envSchema = z.object({
  OPENROUTER_API_KEY: z.string().min(1, "OPENROUTER_API_KEY is required"),
  OPENROUTER_BASE_URL: z
    .string()
    .url()
    .default("https://openrouter.ai/api/v1"),
  OPENROUTER_MODEL: z.string().min(1),
  OPENROUTER_TEMPERATURE: z.coerce.number().min(0).max(2).default(0.2),
  OPENROUTER_MAX_TOKENS: z.coerce.number().int().positive().default(4000),
  OPENROUTER_TIMEOUT_MS: z.coerce.number().int().positive().default(60000),
  OPENROUTER_MAX_RETRIES: z.coerce.number().int().min(0).default(3),

  REQUIREMENT_AGENT_MODEL: z.string().optional(),
  PLANNER_AGENT_MODEL: z.string().optional(),
  TESTCASE_AGENT_MODEL: z.string().optional(),
  EXECUTOR_AGENT_MODEL: z.string().optional(),
  TRIAGE_AGENT_MODEL: z.string().optional(),
  REPORTER_AGENT_MODEL: z.string().optional(),

  DEEPEVAL_JUDGE_MODEL: z.string().min(1),
  DEEPEVAL_METRIC_THRESHOLD: z.coerce.number().min(0).max(1).default(0.6),
  DEEPEVAL_GATE_ON_FAILURE: z
    .enum(["true", "false"])
    .default("true")
    .transform((v) => v === "true"),
  DEEPEVAL_MAX_RETRIES_ON_FAIL: z.coerce.number().int().min(0).default(1),
  EVAL_SERVICE_URL: z.string().url().default("http://localhost:8000"),
  EVAL_TIMEOUT_MS: z.coerce.number().int().positive().default(90000),

  DATABASE_URL: z.string().min(1, "DATABASE_URL is required"),
  DATABASE_URL_UNPOOLED: z.string().optional(),
  TARGET_APP_URL: z.string().optional(),
  MAX_SELF_HEAL_RETRIES: z.coerce.number().int().min(0).default(2),
  MAX_AGENT_ITERATIONS: z.coerce.number().int().positive().default(6),
  TEST_EXECUTION_TIMEOUT_MS: z.coerce.number().int().positive().default(120000),
  PLAYWRIGHT_HEADLESS: z
    .enum(["true", "false"])
    .default("true")
    .transform((v) => v === "true"),

  NEXT_PUBLIC_APP_NAME: z.string().default("QA STLC Studio"),
  NODE_ENV: z.string().default("development"),
  VERCEL_URL: z.string().optional(),
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  const missing = parsed.error.issues
    .map((i) => i.path.join("."))
    .join(", ");
  throw new Error(
    `Invalid or missing environment variables: ${missing}. ` +
      `Copy .env.example to .env and fill in the values.`
  );
}

export const env = parsed.data;
