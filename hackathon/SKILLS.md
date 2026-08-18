---
name: ai-qa-stlc-system
description: Build a complete, end-to-end AI-powered Software Testing Life Cycle (STLC) system — requirement analysis, test planning, test case generation, self-healing test execution, defect triage, and reporting, each handled by a dedicated AI agent, each evaluated by DeepEval, orchestrated end to end, backed by Neon Postgres, deployed on Vercel, with a Claude-style beige UI and collapsible left navigation. Use this skill whenever the user wants to scaffold, extend, or deploy this specific product, wants a new agent added to the pipeline, wants the eval layer wired up, or references "the QA STLC system," "SKILLS.md," or this blueprint by name.
---

# AI-Powered QA STLC System — Build Blueprint

This document is a complete, self-contained spec. Follow it top to bottom to scaffold a working, deployable product. Every value that could change between environments (API keys, model names, thresholds, URLs) is a `.env` variable — **never hardcode any of these in application code.**

---

## 1. What we're building

A web app where each phase of the Software Testing Life Cycle (STLC) is handled by a dedicated AI agent. Agents hand context to each other through a shared Postgres store. Every agent's output is scored by DeepEval before it's allowed to move to the next phase. The user drives everything from a dashboard: paste a requirement, click one button, and watch the pipeline run — planning, test case generation, execution, triage, and a final report — with eval scores visible at every step.

**Agents (STLC mapping):**

| # | Agent | Phase | Input | Output |
|---|-------|-------|-------|--------|
| 1 | `requirement-agent` | Requirement Analysis | Raw requirement/ticket text | Structured requirements + ambiguity flags (RTM seed) |
| 2 | `planner-agent` | Test Planning | Requirements | Test plan: scope, risk areas, test types needed |
| 3 | `testcase-agent` | Test Case Design | Test plan + requirements | Structured test cases (Gherkin-style), each linked to a requirement ID |
| 4 | `executor-agent` | Test Execution | Test cases + target app URL | Playwright scripts, run results, screenshots, self-healing patch log |
| 5 | `triage-agent` | Defect Reporting | Failed test results | Structured bug reports, duplicate flags, root-cause hypotheses |
| 6 | `reporter-agent` | Test Closure | Full run: requirements → results → defects | Executive summary, coverage vs RTM, risk gaps, flaky trends |

An `orchestrator` (state machine, not a heavy framework) drives agents in sequence, persists every step, and calls DeepEval after each agent to gate progression.

---

## 2. Tech stack

- **Frontend + API**: Next.js 14+ (App Router), TypeScript, deployed on Vercel
- **UI**: Tailwind CSS, shadcn/ui primitives, Claude-style beige theme (see §10)
- **Database**: Neon Postgres (serverless, via `@neondatabase/serverless`), provisioned at console.neon.tech
- **ORM**: Drizzle ORM (lightweight, serverless-friendly, good with Neon's HTTP driver)
- **LLM provider**: OpenRouter, default model `nvidia/nemotron-nano-9b-v2:free`, fully swappable via `.env`
- **Eval**: DeepEval (Python) — see §8 for how it bridges into the mostly-TypeScript app
- **Browser automation**: Playwright, via `playwright-core` + `@sparticuz/chromium` for Vercel-compatible serverless execution
- **Vector store (for triage duplicate-detection + reporter grounding)**: Neon's `pgvector` extension — no separate vector DB service needed

---

## 3. Environment configuration — `.env.example`

Create this file at the project root. **Nothing below is ever hardcoded elsewhere in the codebase** — every agent, every DB call, every model reference reads from `process.env`.

```env
# ---------- OpenRouter (LLM provider) ----------
OPENROUTER_API_KEY=
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_MODEL=nvidia/nemotron-nano-9b-v2:free
OPENROUTER_TEMPERATURE=0.2
OPENROUTER_MAX_TOKENS=4000
OPENROUTER_TIMEOUT_MS=60000
OPENROUTER_MAX_RETRIES=3

# Per-agent model overrides (optional — falls back to OPENROUTER_MODEL if unset)
# Lets you use a stronger free model for judgment-heavy agents if needed.
REQUIREMENT_AGENT_MODEL=
PLANNER_AGENT_MODEL=
TESTCASE_AGENT_MODEL=
EXECUTOR_AGENT_MODEL=
TRIAGE_AGENT_MODEL=
REPORTER_AGENT_MODEL=

# ---------- DeepEval (judge layer) ----------
# Judge model should ideally differ from the generation model to reduce
# self-grading bias. Defaults to OPENROUTER_MODEL if unset.
DEEPEVAL_JUDGE_MODEL=nvidia/nemotron-nano-9b-v2:free
DEEPEVAL_METRIC_THRESHOLD=0.6
DEEPEVAL_GATE_ON_FAILURE=true
DEEPEVAL_MAX_RETRIES_ON_FAIL=1

# ---------- Neon Database ----------
DATABASE_URL=postgresql://<user>:<password>@<neon-host>/<db>?sslmode=require
DATABASE_URL_UNPOOLED=postgresql://<user>:<password>@<neon-host-direct>/<db>?sslmode=require

# ---------- Test execution ----------
TARGET_APP_URL=
MAX_SELF_HEAL_RETRIES=2
MAX_AGENT_ITERATIONS=6
TEST_EXECUTION_TIMEOUT_MS=120000
PLAYWRIGHT_HEADLESS=true

# ---------- App ----------
NEXT_PUBLIC_APP_NAME=QA STLC Studio
NODE_ENV=development
VERCEL_URL=
```

Provide a `lib/env.ts` that validates all of these with `zod` at startup and throws a clear error naming the missing var — never let a missing key silently fall through to a hardcoded default.

---

## 4. Database schema (Neon Postgres)

Run this as an initial Drizzle migration. Enable `pgvector` first.

```sql
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
  status TEXT NOT NULL DEFAULT 'pending', -- pending|running|passed|failed|blocked
  current_phase TEXT,                     -- requirement|planning|testcase|execution|triage|reporting
  raw_requirement TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  completed_at TIMESTAMPTZ
);

CREATE TABLE requirements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  req_key TEXT NOT NULL,          -- e.g. REQ-001
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
  test_types JSONB               -- ["functional","regression","api","ui"]
);

CREATE TABLE test_cases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pipeline_run_id UUID REFERENCES pipeline_runs(id),
  requirement_id UUID REFERENCES requirements(id),
  title TEXT NOT NULL,
  gherkin TEXT NOT NULL,
  case_type TEXT,                 -- positive|negative|edge|boundary
  generated_script TEXT,          -- Playwright script text
  status TEXT DEFAULT 'not_run'   -- not_run|passed|failed|healed|blocked
);

CREATE TABLE test_executions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  test_case_id UUID REFERENCES test_cases(id),
  run_number INT DEFAULT 1,
  status TEXT,                    -- passed|failed|healed
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
  severity TEXT,                  -- critical|high|medium|low
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
  model_override TEXT,           -- optional, falls back to .env default
  enabled BOOLEAN DEFAULT true
);
```

Indexes worth adding: `pipeline_run_id` on every child table, and an `ivfflat` index on `defects.embedding` for similarity search once you have enough rows.

---

## 5. Project structure

```
qa-stlc-system/
├── .env.example
├── SKILLS.md                        (this file)
├── package.json
├── drizzle.config.ts
├── vercel.json
├── app/
│   ├── layout.tsx                   (beige theme shell + left nav)
│   ├── page.tsx                     (Dashboard)
│   ├── pipelines/
│   │   ├── page.tsx                 (list + one-click "New Pipeline")
│   │   └── [id]/page.tsx            (live pipeline view, per-phase status)
│   ├── agents/
│   │   ├── page.tsx                 (agent registry — enable/disable, edit prompts)
│   ├── execution/
│   │   └── page.tsx                 (test execution results, screenshots, logs)
│   ├── defects/
│   │   └── page.tsx                 (defect list, duplicates, severity)
│   ├── reports/
│   │   └── page.tsx                 (coverage, trends, eval score history)
│   ├── settings/
│   │   └── page.tsx                 (read-only display of active .env config)
│   └── api/
│       ├── pipelines/route.ts               (POST create + run)
│       ├── pipelines/[id]/route.ts          (GET status)
│       ├── agents/route.ts                  (CRUD agent_definitions)
│       ├── agents/generate/route.ts         (one-click agent scaffolding, §11)
│       ├── execution/run/route.ts           (trigger Playwright run)
│       └── eval/route.ts                    (GET eval_scores for a run)
├── lib/
│   ├── env.ts                       (zod-validated env loader)
│   ├── db/
│   │   ├── schema.ts                (Drizzle schema, mirrors §4)
│   │   └── client.ts                (Neon connection)
│   ├── openrouter/
│   │   └── client.ts                (single shared client, §6)
│   ├── agents/
│   │   ├── orchestrator.ts          (state machine, §7)
│   │   ├── requirement-agent.ts
│   │   ├── planner-agent.ts
│   │   ├── testcase-agent.ts
│   │   ├── executor-agent.ts
│   │   ├── triage-agent.ts
│   │   └── reporter-agent.ts
│   └── eval/
│       ├── run_evals.py             (DeepEval entrypoint, §8)
│       └── openrouter_judge.py      (custom DeepEval LLM wrapper)
└── components/
    ├── nav/CollapsibleSidebar.tsx
    ├── pipeline/PhaseTimeline.tsx
    ├── pipeline/EvalBadge.tsx
    └── ui/*                         (shadcn primitives, beige tokens)
```

---

## 6. OpenRouter client — `lib/openrouter/client.ts`

One shared client, fully parameterized, used by every agent:

```ts
import { env } from "@/lib/env";

export async function callOpenRouter(
  messages: { role: string; content: string }[],
  opts?: { model?: string; temperature?: number; maxTokens?: number }
) {
  const model = opts?.model || env.OPENROUTER_MODEL;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), env.OPENROUTER_TIMEOUT_MS);

  try {
    const res = await fetch(`${env.OPENROUTER_BASE_URL}/chat/completions`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        messages,
        temperature: opts?.temperature ?? env.OPENROUTER_TEMPERATURE,
        max_tokens: opts?.maxTokens ?? env.OPENROUTER_MAX_TOKENS,
      }),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`OpenRouter error ${res.status}: ${await res.text()}`);
    const data = await res.json();
    return data.choices[0].message.content as string;
  } finally {
    clearTimeout(timeout);
  }
}
```

Wrap every agent call with retry (`OPENROUTER_MAX_RETRIES`) and exponential backoff — the free-tier model will rate-limit under load; this is expected, not a bug.

---

## 7. Orchestrator — the state machine

`lib/agents/orchestrator.ts` drives phases in order. Each phase follows the same loop:

```
1. Load context (previous phase outputs from DB)
2. Call agent → structured output
3. Persist output
4. Trigger DeepEval scoring for this phase (§8)
5. If DEEPEVAL_GATE_ON_FAILURE=true and score < threshold:
     retry once (DEEPEVAL_MAX_RETRIES_ON_FAIL) with failure reasoning appended to prompt
     if still failing → mark pipeline_run.status = 'blocked', stop, surface to UI
6. Else advance current_phase to next
```

Phase order: `requirement → planning → testcase → execution → triage → reporting`.

Each agent file exports one function with this shape, so the orchestrator can call them uniformly:

```ts
export async function run(input: PhaseInput): Promise<{ output: unknown; rawText: string }>
```

Agents must return **structured JSON** (validated with `zod`), not free text — this is what makes DeepEval scoring and DB persistence reliable. Instruct the model explicitly in the system prompt to return JSON only, and add a JSON-repair retry (strip markdown fences, retry parse once) since free-tier models are less reliable at strict JSON than larger models.

---

## 8. DeepEval integration

DeepEval is Python; the app is TypeScript. Bridge them with a small Python microservice (FastAPI, one route: `POST /evaluate`) that the Next.js API routes call over HTTP. Keep it in `lib/eval/` and deploy it as a **separate Vercel-adjacent service** (e.g., a small Fly.io/Render instance, or a Vercel Python function if execution time allows) — DeepEval's judge calls can be slow on free models, so don't force this into the same short-timeout serverless function as the UI.

**Custom OpenRouter judge — `lib/eval/openrouter_judge.py`:**

```python
import os, requests
from deepeval.models import DeepEvalBaseLLM

class OpenRouterJudge(DeepEvalBaseLLM):
    def __init__(self):
        self.model_name = os.environ["DEEPEVAL_JUDGE_MODEL"]
        self.api_key = os.environ["OPENROUTER_API_KEY"]
        self.base_url = os.environ["OPENROUTER_BASE_URL"]

    def load_model(self):
        return self.model_name

    def generate(self, prompt: str) -> str:
        resp = requests.post(
            f"{self.base_url}/chat/completions",
            headers={"Authorization": f"Bearer {self.api_key}"},
            json={
                "model": self.model_name,
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0,
            },
            timeout=60,
        )
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"]

    async def a_generate(self, prompt: str) -> str:
        return self.generate(prompt)

    def get_model_name(self):
        return self.model_name
```

**Metric-per-agent mapping** (`lib/eval/run_evals.py`), all using `GEval` with `threshold=os.environ["DEEPEVAL_METRIC_THRESHOLD"]` and `model=OpenRouterJudge()`:

| Agent | GEval criteria (evaluation_steps) |
|---|---|
| requirement-agent | Checks every stated requirement is captured; flags whether genuinely ambiguous items were actually flagged; penalizes invented requirements not present in source text |
| planner-agent | Checks risk areas are grounded in the actual requirement content, not generic boilerplate; checks test types chosen are justified |
| testcase-agent | Checks every test case traces to a real requirement ID; checks positive/negative/edge/boundary coverage is present, not just happy-path |
| executor-agent | Uses DeepEval's task-completion/trajectory pattern: did the tool-call sequence achieve the stated test goal, not just "did it not crash" |
| triage-agent | Checks bug report has concrete repro steps, plausible severity, and the root-cause hypothesis is grounded in cited evidence (git/PR/log refs), not fabricated |
| reporter-agent | Faithfulness check: every claim in the summary must be traceable to underlying run data — this is the highest-stakes agent, since it's what a human trusts |

The FastAPI service persists results directly into `eval_scores` (same `DATABASE_URL`) so the Next.js UI just reads them, no extra plumbing needed.

**Important honest caveat to build in, not paper over**: DeepEval's structured-output scoring can fail on small free models with `ValueError: Evaluation LLM outputted an invalid JSON`. Handle this explicitly — catch it, retry once, and if it still fails, mark that eval score as `null`/`errored` in the UI rather than silently treating it as a pass or a crash.

---

## 9. API routes (Next.js)

- `POST /api/pipelines` — body `{ projectId, rawRequirement }` → creates `pipeline_runs` row, kicks off orchestrator (this is the "one-click" trigger)
- `GET /api/pipelines/[id]` — full run state: phase, requirements, test plan, test cases, executions, defects, eval scores — powers the live pipeline view
- `POST /api/agents/generate` — one-click agent scaffolding (§11)
- `POST /api/execution/run` — triggers the executor-agent + Playwright for a given `pipeline_run_id`
- `GET /api/eval?pipelineRunId=` — eval score history for charts

All routes read config exclusively from `lib/env.ts` — no inline `process.env.X` scattered through route handlers.

---

## 10. UI spec — Claude-style beige theme, collapsible left nav

**Design tokens** (Tailwind config / CSS variables):

```css
--bg-primary: #F5F0E8;      /* warm beige page background */
--bg-surface: #FBF8F3;      /* card/panel surface, slightly lighter */
--bg-sidebar: #EDE6D8;      /* left nav, one shade darker than page */
--text-primary: #2B2A27;
--text-muted: #6B6558;
--accent: #C96442;          /* warm terracotta, used sparingly for CTAs/active states */
--border: #E2D9C7;
--success: #4F7942;
--warning: #C9A227;
--danger: #B3452C;
--radius: 0.75rem;
```

Typography: a warm serif or humanist sans for headings (e.g. `"Tiempos", "Georgia", serif` fallback stack), clean sans (`Inter` or system-ui) for body — mirrors the calm, editorial feel rather than a typical dense dev-tool dashboard.

**Left navigation** (`components/nav/CollapsibleSidebar.tsx`):
- Collapsible (icon-only ↔ expanded), state persisted in `localStorage`... actually, per artifact rules, avoid localStorage-only reliance for the deployed app; store the collapse preference as a simple React state / cookie, not a hard dependency.
- Sections, top to bottom:
  1. **Dashboard** — active runs, eval score trend sparkline, recent defects
  2. **Pipelines** — list of runs + big "New Pipeline" button (one-click generation entry point)
  3. **Agents** — registry of the 6 agents, enable/disable toggle, prompt editor, model override per agent
  4. **Test Execution** — live/most-recent run results, screenshots, self-heal log
  5. **Defects** — triaged bugs, duplicate clusters, severity filter
  6. **Reports & Analysis** — coverage vs RTM, flaky-test trends over time, eval score history charts
  7. **Settings** — read-only view of which `.env` vars are set (never display secret values, just "configured / missing")

**Pipeline detail view** (`app/pipelines/[id]/page.tsx`): a horizontal phase timeline (`PhaseTimeline.tsx`) showing all 6 phases as steps; each step shows status (pending/running/passed/blocked) and an `EvalBadge` with the DeepEval score once available. Clicking a phase expands its output panel (requirements table, test plan, test cases, execution results, defects, or the final report, depending on phase).

Keep every page working with real data from the API routes above — no mocked placeholder state left in the shipped UI.

---

## 11. One-click agent generation

`POST /api/agents/generate` is the "generate multiple AI agents" entry point referenced in the brief. Behavior:

1. Accepts an optional `{ projectDescription }` to tailor prompts (e.g., "e-commerce checkout flow" vs "internal admin tool") — if omitted, uses sensible generic STLC defaults.
2. For each of the 6 agent roles, calls OpenRouter once with a **meta-prompt** asking it to draft a tailored system prompt for that role given the project description.
3. Inserts one row per agent into `agent_definitions` (creating them if missing, updating `system_prompt` if the user re-runs generation).
4. Returns the full set to the UI, which renders them on the **Agents** page for review/edit before the user runs a pipeline.

This keeps agent behavior data-driven (stored in Postgres, editable in the UI) rather than hardcoded in source files — so the whole agent roster is itself a "generated, configurable" thing, not just the `.env` values.

---

## 12. Deployment

**Neon (console.neon.tech):**
1. Create a project, copy the pooled connection string into `DATABASE_URL` and the direct one into `DATABASE_URL_UNPOOLED`.
2. Enable the `vector` extension from the Neon SQL editor before running migrations.
3. Run `npx drizzle-kit push` to apply the schema in §4.

**Vercel:**
1. Import the repo, set every variable from `.env.example` in Project Settings → Environment Variables (production + preview).
2. `vercel.json` — extend serverless timeouts for the execution route, since Playwright runs and LLM calls on free models can be slow:
   ```json
   {
     "functions": {
       "app/api/execution/run/route.ts": { "maxDuration": 300 },
       "app/api/pipelines/route.ts": { "maxDuration": 120 }
     }
   }
   ```
3. **Honest constraint**: Vercel's serverless functions are not built for long-running headless-browser sessions. For a real deployment, use `playwright-core` + `@sparticuz/chromium` (works within Vercel's function size/time limits for short, single-page test runs) and keep individual test executions short. For heavier suites, run the `executor-agent` as a separate worker (e.g., a small Fly.io/Railway service) that Vercel's API route calls and polls — don't try to force multi-minute browser sessions into a single Vercel invocation.
4. The DeepEval Python service (§8) is deployed separately (it isn't a Next.js route) — point `EVAL_SERVICE_URL` (add this to `.env`) at wherever it's hosted.

---

## 13. Build order (do it in this sequence, each stage demoable on its own)

1. **Scaffold**: Next.js + Tailwind + shadcn, `.env.example`, `lib/env.ts`, Neon connection, Drizzle schema + migration
2. **UI shell**: beige theme tokens, collapsible sidebar, empty page routes for all 7 sections
3. **Requirement → Test Case pipeline** (agents 1–3, orchestrator phases 1–3 only): prove the JSON-structured agent pattern works end to end, visible in the Pipelines/agent detail view
4. **DeepEval gate**: wire the Python eval service, hook it after phases 1–3, show `EvalBadge` scores in the UI — get the gating/retry loop solid before adding more agents
5. **Executor agent + self-healing**: Playwright integration, screenshot capture, selector-repair loop
6. **Triage + Reporter agents**: closes the full STLC loop, pgvector duplicate detection, final report view
7. **One-click agent generation** (§11): layer this in once the manual agent flow is proven
8. **Deploy**: Neon + Vercel + separate eval service, verify the full pipeline runs against a real `TARGET_APP_URL` end to end in production

---

## 14. Verification checklist before calling it done

- [ ] Grep the codebase for hardcoded API keys, model names, or URLs — there should be none outside `.env.example`
- [ ] Run a pipeline against a deliberately vague requirement — confirm `requirement-agent` flags ambiguity instead of guessing
- [ ] Force a selector break on the target app — confirm `executor-agent` self-heals and logs the diff, doesn't silently pass
- [ ] Confirm a low DeepEval score actually blocks the pipeline when `DEEPEVAL_GATE_ON_FAILURE=true`, and that toggling it to `false` just logs instead of blocking
- [ ] Confirm `reporter-agent` output has no claims that don't trace back to `eval_scores`/`test_executions`/`defects` rows
- [ ] Load the deployed Vercel URL — sidebar collapses, all 7 sections render with real (not mocked) data
