# AI-Powered QA STLC System

> An end-to-end AI-driven Software Testing Life Cycle (STLC) platform where six dedicated AI agents handle requirement analysis, test planning, test case design, test execution, defect triage, and reporting — each phase gated by a DeepEval quality score.

## Description

A web app where each phase of the Software Testing Life Cycle (STLC) is handled by a dedicated AI agent. Agents hand context to each other through a shared Postgres store. Every agent's output is scored by DeepEval before it's allowed to move to the next phase. The user drives everything from a dashboard: paste a requirement, click one button, and watch the pipeline run — planning, test case generation, execution, triage, and a final report — with eval scores visible at every step.

## Problem Statement

Manual and traditional test automation has several bottlenecks:

- **Slow feedback**: translating a requirement/ticket into test cases, plans, and coverage reports is manual, tedious, and error-prone.
- **No quality gate on artifacts**: there is no objective check that generated requirements, plans, or test cases are complete, traceable, and grounded in the source text — so low-quality artifacts silently flow downstream.
- **Poor traceability**: test cases rarely link back to requirement IDs, making Requirement Traceability Matrix (RTM) coverage reporting a manual chore.
- **No self-healing**: when UI selectors break, test suites fail and stay failed until a human rewrites them.
- **Scattered context**: each STLC phase lives in separate tools and documents; nothing hands context cleanly from one phase to the next.

## Solution

**QA STLC Studio** automates the entire STLC with a pipeline of six AI agents, each with a narrow, well-defined job. A state-machine orchestrator drives them in sequence, persists every artifact to Postgres, and gates each phase with a DeepEval GEval score before progression. This gives test teams:

- **One-click requirement → report**: paste a requirement, get structured requirements, a test plan, Gherkin test cases, executable Playwright scripts, triaged defects, and an executive summary.
- **Eval-scored artifacts**: every phase output is scored (0–1) against per-agent criteria; low scores block the run instead of silently passing.
- **Grounded, traceable output**: test cases link to real requirement IDs; the reporter's claims must trace to actual run data.
- **Self-healing execution**: the executor retries failed Playwright runs (`MAX_SELF_HEAL_RETRIES`) and logs the selector-repair diff.
- **Fully configurable via `.env`**: API keys, model names, thresholds, timeouts — nothing hardcoded, all validated at startup by `lib/env.ts` (zod).

## Agents (STLC mapping)

| # | Agent | Phase | Input | Output |
|---|-------|-------|-------|--------|
| 1 | `requirement-agent` | Requirement Analysis | Raw requirement/ticket text | Structured requirements + ambiguity flags (RTM seed) |
| 2 | `planner-agent` | Test Planning | Requirements | Test plan: scope, risk areas, test types |
| 3 | `testcase-agent` | Test Case Design | Test plan + requirements | Structured test cases (Gherkin), linked to requirement IDs |
| 4 | `executor-agent` | Test Execution | Test cases + target app URL | Playwright scripts, run results, self-healing patch log |
| 5 | `triage-agent` | Defect Reporting | Failed test results | Structured bug reports, duplicate flags, root-cause hypotheses |
| 6 | `reporter-agent` | Test Closure | Full run | Executive summary, coverage vs RTM, risk gaps, flaky trends |

An `orchestrator` (state machine) drives agents in sequence, persists every step, and calls DeepEval after each agent to gate progression.

## Flow Diagram

```
                     ┌────────────────────────────┐
                     │  User pastes requirement   │
                     │  (POST /api/pipelines)     │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  1. requirement-agent      │
                     │  → structured requirements │
                     │  → ambiguity flags         │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  2. planner-agent          │
                     │  → test plan (scope, risk, │
                     │    test types)             │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  3. testcase-agent         │
                     │  → Gherkin test cases      │
                     │  linked to requirement IDs │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  4. executor-agent         │
                     │  → Playwright scripts      │
                     │  → run + self-heal loop    │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  5. triage-agent           │
                     │  → bug reports, severity,  │
                     │    duplicate flags         │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  6. reporter-agent         │
                     │  → executive summary,      │
                     │    coverage vs RTM         │
                     └─────────────┬──────────────┘
                                   ▼
                     ┌────────────────────────────┐
                     │  Pipeline marked "passed"  │
                     │  (status + eval badges)    │
                     └────────────────────────────┘

   ┌─────────────────────────────────────────────────────┐
   │  EVERY phase: persist to Postgres → DeepEval GEval  │
   │  score → below threshold? retry once → still low?   │
   │  mark run "blocked" and stop.                       │
   └─────────────────────────────────────────────────────┘
```

## Tech stack

- **Frontend + API**: Next.js 16 (App Router), TypeScript, deployed on Vercel
- **UI**: Tailwind CSS, warm beige editorial theme, collapsible left nav
- **Database**: Neon Postgres (serverless) via Drizzle ORM, `pgvector` for embeddings
- **LLM provider**: OpenRouter, default model `nvidia/nemotron-nano-9b-v2:free`, fully swappable via `.env`
- **Eval**: DeepEval (Python microservice) — GEval scores gating each phase
- **Browser automation**: Playwright via `playwright-core` + `@sparticuz/chromium`

## Key features

- **One-click pipeline**: requirement → report with a single button.
- **DeepEval quality gates**: each phase scored; low scores block progression (`DEEPEVAL_GATE_ON_FAILURE`).
- **JSON-repair retry**: free-tier models occasionally truncate JSON; `callWithJsonRepair` asks the model to fix it automatically.
- **Live UI**: poll-based pipeline detail page with phase timeline, eval badges, stop/cancel button, and visible errors.
- **Self-healing execution**: Playwright runner with retry loop and serverless Chromium (`@sparticuz/chromium`).
- **Agent registry**: enable/disable agents, edit prompts, or regenerate prompts with one click via OpenRouter.
- **Async kickoff**: `POST /api/pipelines` returns in ~0.5s; the run proceeds in the background while the UI polls.

## Demo — sample run

Paste this requirement into the **Pipelines** page and click **Run Pipeline**:

> As a user, I can log in with email and password. Wrong password should show an error. Account locks after 5 failed attempts.

What the pipeline produces, phase by phase:

1. **Requirement analysis** → 3 structured requirements (`REQ-001` login, `REQ-002` wrong-password error, `REQ-003` account lock), each with acceptance criteria.
2. **Test planning** → scope, risk areas (auth, lockout), test types (functional, negative, boundary).
3. **Test case design** → ~8 Gherkin test cases covering positive, negative, edge, and boundary scenarios, each traced to a requirement ID.
4. **Execution** (needs `TARGET_APP_URL`) → Playwright scripts run against the target app; failures retry with self-healing.
5. **Triage** → structured defect reports with severity and root-cause hypotheses for any failures.
6. **Reporting** → executive summary, RTM coverage percentage, risk gaps, flaky trends.

The timeline on the pipeline detail page shows each phase's status (pending → running → passed/blocked) with DeepEval score badges.

## Project structure

```
qa-stlc-system/
├── app/                  # Next.js App Router (pages + API routes)
│   ├── pipelines/        # run list + detail (live polling, cancel)
│   ├── agents/           # agent registry + one-click generation
│   ├── execution/        # test execution results
│   ├── defects/          # triaged defects
│   ├── reports/          # coverage, trends, eval history
│   └── settings/         # env config status
├── components/           # sidebar, timeline, evals, ui primitives
├── lib/
│   ├── env.ts            # zod-validated env loader
│   ├── db/               # Drizzle schema + Neon client
│   ├── openrouter/       # shared LLM client + JSON parsing/repair
│   ├── agents/           # 6 agents + orchestrator state machine
│   ├── eval/             # DeepEval Python service (FastAPI)
│   └── execution/        # Playwright runner + self-healing
└── drizzle/              # initial migration SQL
```

## Database schema

| Table | Purpose |
|-------|---------|
| `projects` | Project metadata + target app URL |
| `pipeline_runs` | One row per run: status, current phase, raw requirement, error |
| `requirements` | Structured requirements (RTM seed), ambiguity flags |
| `test_plans` | Scope, risk areas, test types |
| `test_cases` | Gherkin cases, case type, status, linked `requirement_id` |
| `test_executions` | Run results, duration, logs, self-heal diff |
| `defects` | Triaged bugs, severity, root-cause hypothesis, `vector(768)` embedding |
| `eval_scores` | Per-agent GEval scores, thresholds, pass/fail |
| `agent_definitions` | Editable agent prompts, model overrides, enabled flags |

Schema is defined in `lib/db/schema.ts` (Drizzle) and mirrored in `drizzle/0000_initial.sql`. The `pgvector` extension powers duplicate-defect similarity search on `defects.embedding`.

## Getting started

### 1. Environment

```bash
cp .env.example .env
# fill in OPENROUTER_API_KEY, DATABASE_URL, TARGET_APP_URL
```

Everything is parameterized through `.env` — validated at startup by `lib/env.ts` (zod). No hardcoded keys, model names, or URLs in application code.

### 2. Database (Neon Postgres)

1. Create a project at console.neon.tech
2. Run `CREATE EXTENSION IF NOT EXISTS vector;` in the SQL editor
3. Apply schema:

```bash
npm run db:push
```

### 3. Eval service (DeepEval, Python)

```bash
cd lib/eval
pip install -r requirements.txt
uvicorn main:app --port 8000
```

Set `EVAL_SERVICE_URL=http://localhost:8000` in `.env`.

### 4. Run the app

```bash
npm install
npm run dev
```

Open http://localhost:3000, go to **Pipelines**, paste a requirement, hit **Run Pipeline**. The UI polls the run live: phase timeline, eval badges, requirements, test cases, executions, defects, and the final report all populate as the pipeline progresses.

## How it works

1. `POST /api/pipelines` creates a `pipeline_runs` row and kicks off the orchestrator (fire-and-forget — responds in ~0.5s).
2. Each agent runs in sequence: requirement → planning → testcase → execution → triage → reporting.
3. After each agent, the DeepEval service scores the output against a per-agent GEval criteria.
4. If `DEEPEVAL_GATE_ON_FAILURE=true` and score < threshold, the phase retries once (`DEEPEVAL_MAX_RETRIES_ON_FAIL`); if still failing, the run is marked `blocked`.
5. Execution runs generated Playwright scripts with a self-healing retry loop (`MAX_SELF_HEAL_RETRIES`).
6. The UI polls `GET /api/pipelines/[id]` for live status and eval badges. Runs can be cancelled via `POST /api/pipelines/cancel`.

## API routes

| Route | Purpose |
|-------|---------|
| `POST /api/pipelines` | Create project + kick off orchestrator (async) |
| `GET /api/pipelines` | List all runs |
| `GET /api/pipelines/[id]` | Full run state: requirements, plan, cases, executions, defects, evals |
| `POST /api/pipelines/cancel` | Stop a running pipeline |
| `GET/POST/PATCH /api/agents` | Agent registry CRUD |
| `POST /api/agents/generate` | One-click agent prompt generation |
| `POST /api/execution/run` | Trigger Playwright execution for a run |
| `GET /api/eval?pipelineRunId=` | Eval score history |

## Deployment

- **Neon**: provision at console.neon.tech, set `DATABASE_URL` + `DATABASE_URL_UNPOOLED`
- **Vercel**: import repo, set env vars from `.env.example`, `vercel.json` extends timeouts for the execution/pipeline routes
- **Eval service**: deploy `lib/eval/` separately (Fly.io/Render/Vercel Python function), point `EVAL_SERVICE_URL` at it
- **Honest constraint**: Vercel serverless functions aren't built for long browser sessions. Keep individual test executions short with `@sparticuz/chromium`; for heavy suites run the executor as a separate worker.

## Verification checklist

- [ ] No hardcoded API keys/model names/URLs outside `.env.example`
- [ ] Vague requirement → `requirement-agent` flags ambiguity instead of guessing
- [ ] Selector break → `executor-agent` self-heals and logs the diff
- [ ] Low DeepEval score blocks pipeline when `DEEPEVAL_GATE_ON_FAILURE=true`
- [ ] `reporter-agent` claims trace back to run data
- [ ] All 7 sections render with real (not mocked) data
