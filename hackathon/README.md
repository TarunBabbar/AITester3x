# AI-Powered QA STLC System

A web app where each phase of the Software Testing Life Cycle (STLC) is handled by a dedicated AI agent. Agents hand context to each other through a shared Postgres store. Every agent's output is scored by DeepEval before it's allowed to move to the next phase. Paste a requirement, click one button, and watch the pipeline run — planning, test case generation, execution, triage, and a final report — with eval scores visible at every step.

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

## Tech stack

- **Frontend + API**: Next.js 16 (App Router), TypeScript, deployed on Vercel
- **UI**: Tailwind CSS, Claude-style beige theme, collapsible left nav
- **Database**: Neon Postgres (serverless) via Drizzle ORM, `pgvector` for embeddings
- **LLM provider**: OpenRouter, default model `nvidia/nemotron-nano-9b-v2:free`, fully swappable via `.env`
- **Eval**: DeepEval (Python microservice) — GEval scores gating each phase
- **Browser automation**: Playwright via `playwright-core` + `@sparticuz/chromium`

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

Open http://localhost:3000, go to **Pipelines**, paste a requirement, hit **Run Pipeline**.

## How it works

1. `POST /api/pipelines` creates a `pipeline_runs` row and kicks off the orchestrator
2. Each agent runs in sequence: requirement → planning → testcase → execution → triage → reporting
3. After each agent, the DeepEval service scores the output against a per-agent GEval criteria
4. If `DEEPEVAL_GATE_ON_FAILURE=true` and score < threshold, the phase retries once (`DEEPEVAL_MAX_RETRIES_ON_FAIL`); if still failing, the run is marked `blocked`
5. Execution runs generated Playwright scripts with a self-healing retry loop (`MAX_SELF_HEAL_RETRIES`)
6. The UI polls `GET /api/pipelines/[id]` for live status and eval badges

## API routes

| Route | Purpose |
|-------|---------|
| `POST /api/pipelines` | Create project + kick off orchestrator |
| `GET /api/pipelines` | List all runs |
| `GET /api/pipelines/[id]` | Full run state: requirements, plan, cases, executions, defects, evals |
| `GET/POST/PATCH /api/agents` | Agent registry CRUD |
| `POST /api/agents/generate` | One-click agent prompt generation |
| `POST /api/execution/run` | Trigger Playwright execution for a run |
| `GET /api/eval?pipelineRunId=` | Eval score history |

## Project structure

```
qa-stlc-system/
├── app/                  # Next.js App Router (pages + API routes)
│   ├── pipelines/        # run list + detail (live polling)
│   ├── agents/           # agent registry + one-click generation
│   ├── execution/        # test execution results
│   ├── defects/          # triaged defects
│   ├── reports/          # coverage, trends, eval history
│   └── settings/         # env config status
├── components/           # sidebar, timeline, evals, ui primitives
├── lib/
│   ├── env.ts            # zod-validated env loader
│   ├── db/               # Drizzle schema + Neon client
│   ├── openrouter/       # shared LLM client + JSON parsing
│   ├── agents/           # 6 agents + orchestrator state machine
│   ├── eval/             # DeepEval Python service
│   └── execution/        # Playwright runner + self-healing
└── drizzle/              # initial migration SQL
```

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
