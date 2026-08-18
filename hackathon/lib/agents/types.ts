import { z } from "zod";

export interface PhaseInput {
  pipelineRunId: string;
  rawRequirement: string;
  targetAppUrl?: string;
  context: Record<string, unknown>;
}

export interface PhaseOutput {
  output: unknown;
  rawText: string;
}

export const RequirementSchema = z.object({
  req_key: z.string(),
  description: z.string(),
  acceptance_criteria: z.string().optional(),
  is_ambiguous: z.boolean(),
  ambiguity_notes: z.string().optional(),
});

export const RequirementOutputSchema = z.object({
  requirements: z.array(RequirementSchema),
});

export const TestPlanOutputSchema = z.object({
  scope: z.string(),
  risk_areas: z.array(z.string()),
  test_types: z.array(z.string()),
});

export const TestCaseSchema = z.object({
  req_key: z.string(),
  title: z.string(),
  gherkin: z.string(),
  case_type: z.enum(["positive", "negative", "edge", "boundary"]),
});

export const TestCaseOutputSchema = z.object({
  test_cases: z.array(TestCaseSchema),
});

export const ExecutionScriptSchema = z.object({
  test_case_id: z.string(),
  script: z.string(),
});

export const ExecutionOutputSchema = z.object({
  scripts: z.array(ExecutionScriptSchema),
});

export const DefectSchema = z.object({
  title: z.string(),
  repro_steps: z.string(),
  expected: z.string(),
  actual: z.string(),
  severity: z.enum(["critical", "high", "medium", "low"]),
  root_cause_hypothesis: z.string(),
  is_duplicate: z.boolean(),
  duplicate_of_title: z.string().optional(),
});

export const DefectOutputSchema = z.object({
  defects: z.array(DefectSchema),
});

export const ReportOutputSchema = z.object({
  executive_summary: z.string(),
  coverage: z.object({
    requirements_tested: z.number(),
    total_requirements: z.number(),
    percent: z.number(),
  }),
  risk_gaps: z.array(z.string()),
  flaky_trends: z.array(z.string()),
  recommendations: z.array(z.string()),
});
