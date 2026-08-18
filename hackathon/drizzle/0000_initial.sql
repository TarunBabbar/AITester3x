-- Enable pgvector extension (run once before migrations)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE projects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  target_app_url TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE pipeline_runs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id UUID REFERENCES projects(id),
  status TEXT NOT NULL DEFAULT 'pending',
  current_phase TEXT,
  raw_requirement TEXT,
  error TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  completed_at TIMESTAMPTZ
);

CREATE TABLE requirements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  req_key TEXT NOT NULL,
  description TEXT NOT NULL,
  acceptance_criteria TEXT,
  is_ambiguous BOOLEAN DEFAULT false,
  ambiguity_notes TEXT
);

CREATE TABLE test_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  scope TEXT,
  risk_areas JSONB,
  test_types JSONB
);

CREATE TABLE test_cases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  requirement_id UUID REFERENCES requirements(id),
  title TEXT NOT NULL,
  gherkin TEXT NOT NULL,
  case_type TEXT,
  generated_script TEXT,
  status TEXT DEFAULT 'not_run'
);

CREATE TABLE test_executions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  test_case_id UUID REFERENCES test_cases(id),
  run_number INT DEFAULT 1,
  status TEXT,
  duration_ms INT,
  screenshot_url TEXT,
  logs TEXT,
  self_heal_applied BOOLEAN DEFAULT false,
  self_heal_diff TEXT,
  executed_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE defects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  test_execution_id UUID REFERENCES test_executions(id),
  title TEXT NOT NULL,
  repro_steps TEXT,
  expected TEXT,
  actual TEXT,
  severity TEXT,
  root_cause_hypothesis TEXT,
  is_duplicate_of UUID REFERENCES defects(id),
  embedding vector(768)
);

CREATE TABLE eval_scores (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  agent_name TEXT NOT NULL,
  metric_name TEXT NOT NULL,
  score FLOAT NOT NULL,
  threshold FLOAT NOT NULL,
  passed BOOLEAN NOT NULL,
  reasoning TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE agent_definitions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  phase TEXT NOT NULL,
  system_prompt TEXT NOT NULL,
  model_override TEXT,
  enabled BOOLEAN DEFAULT true
);

CREATE INDEX idx_pipeline_runs_project ON pipeline_runs(project_id);
CREATE INDEX idx_requirements_run ON requirements(pipeline_run_id);
CREATE INDEX idx_test_plans_run ON test_plans(pipeline_run_id);
CREATE INDEX idx_test_cases_run ON test_cases(pipeline_run_id);
CREATE INDEX idx_test_executions_case ON test_executions(test_case_id);
CREATE INDEX idx_defects_run ON defects(pipeline_run_id);
CREATE INDEX idx_eval_scores_run ON eval_scores(pipeline_run_id);
