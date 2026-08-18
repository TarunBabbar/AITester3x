import {
  AnyPgColumn,
  boolean,
  integer,
  jsonb,
  pgTable,
  text,
  timestamp,
  uuid,
  vector,
} from "drizzle-orm/pg-core";

export const projects = pgTable("projects", {
  id: uuid("id").primaryKey().defaultRandom(),
  name: text("name").notNull(),
  target_app_url: text("target_app_url"),
  created_at: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

export const pipelineRuns = pgTable("pipeline_runs", {
  id: uuid("id").primaryKey().defaultRandom(),
  project_id: uuid("project_id").references(() => projects.id),
  status: text("status").notNull().default("pending"),
  current_phase: text("current_phase"),
  raw_requirement: text("raw_requirement"),
  error: text("error"),
  created_at: timestamp("created_at", { withTimezone: true }).defaultNow(),
  completed_at: timestamp("completed_at", { withTimezone: true }),
});

export const requirements = pgTable("requirements", {
  id: uuid("id").primaryKey().defaultRandom(),
  pipeline_run_id: uuid("pipeline_run_id").references(() => pipelineRuns.id),
  req_key: text("req_key").notNull(),
  description: text("description").notNull(),
  acceptance_criteria: text("acceptance_criteria"),
  is_ambiguous: boolean("is_ambiguous").default(false),
  ambiguity_notes: text("ambiguity_notes"),
});

export const testPlans = pgTable("test_plans", {
  id: uuid("id").primaryKey().defaultRandom(),
  pipeline_run_id: uuid("pipeline_run_id").references(() => pipelineRuns.id),
  scope: text("scope"),
  risk_areas: jsonb("risk_areas"),
  test_types: jsonb("test_types"),
});

export const testCases = pgTable("test_cases", {
  id: uuid("id").primaryKey().defaultRandom(),
  pipeline_run_id: uuid("pipeline_run_id").references(() => pipelineRuns.id),
  requirement_id: uuid("requirement_id").references(() => requirements.id),
  title: text("title").notNull(),
  gherkin: text("gherkin").notNull(),
  case_type: text("case_type"),
  generated_script: text("generated_script"),
  status: text("status").default("not_run"),
});

export const testExecutions = pgTable("test_executions", {
  id: uuid("id").primaryKey().defaultRandom(),
  test_case_id: uuid("test_case_id").references(() => testCases.id),
  run_number: integer("run_number").default(1),
  status: text("status"),
  duration_ms: integer("duration_ms"),
  screenshot_url: text("screenshot_url"),
  logs: text("logs"),
  self_heal_applied: boolean("self_heal_applied").default(false),
  self_heal_diff: text("self_heal_diff"),
  executed_at: timestamp("executed_at", { withTimezone: true }).defaultNow(),
});

export const defects = pgTable(
  "defects",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    pipeline_run_id: uuid("pipeline_run_id").references(() => pipelineRuns.id),
    test_execution_id: uuid("test_execution_id").references(
      () => testExecutions.id
    ),
    title: text("title").notNull(),
    repro_steps: text("repro_steps"),
    expected: text("expected"),
    actual: text("actual"),
    severity: text("severity"),
    root_cause_hypothesis: text("root_cause_hypothesis"),
    is_duplicate_of: uuid("is_duplicate_of").references(
      (): AnyPgColumn => defects.id
    ),
    embedding: vector("embedding", { dimensions: 768 }),
  },
  (table) => [table.id]
);

export const evalScores = pgTable("eval_scores", {
  id: uuid("id").primaryKey().defaultRandom(),
  pipeline_run_id: uuid("pipeline_run_id").references(() => pipelineRuns.id),
  agent_name: text("agent_name").notNull(),
  metric_name: text("metric_name").notNull(),
  score: integer("score").notNull(),
  threshold: integer("threshold").notNull(),
  passed: boolean("passed").notNull(),
  reasoning: text("reasoning"),
  created_at: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

export const agentDefinitions = pgTable("agent_definitions", {
  id: uuid("id").primaryKey().defaultRandom(),
  name: text("name").notNull(),
  phase: text("phase").notNull(),
  system_prompt: text("system_prompt").notNull(),
  model_override: text("model_override"),
  enabled: boolean("enabled").default(true),
});

export type Project = typeof projects.$inferSelect;
export type PipelineRun = typeof pipelineRuns.$inferSelect;
export type Requirement = typeof requirements.$inferSelect;
export type TestPlan = typeof testPlans.$inferSelect;
export type TestCase = typeof testCases.$inferSelect;
export type TestExecution = typeof testExecutions.$inferSelect;
export type Defect = typeof defects.$inferSelect;
export type EvalScore = typeof evalScores.$inferSelect;
export type AgentDefinition = typeof agentDefinitions.$inferSelect;
