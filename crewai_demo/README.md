# CrewAI QA Demo

A demo web app that uses **CrewAI agents** to automate a QA workflow end-to-end
through a simple UI.

## The workflow (two phases)

```
PHASE 1 — Generate Test Cases
  You type a URL
        │
        ▼
  ┌─────────────────┐   ┌────────────────────┐
  │ Agent 1         │   │ Agent 2            │
  │ Page Reader     │──▶│ Test Case Designer │──▶  structured test cases
  │ (Playwright)    │   │ (P0-P3 + steps)    │     shown in the UI
  └─────────────────┘   └────────────────────┘

PHASE 2 — Automate Selected
  you tick the test cases you want
        │
        ▼
  ┌─────────────────┐   ┌────────────────────┐   ┌──────────────┐
  │ Agent 3         │   │ Agent 4            │   │ Test Runner  │
  │ POM Writer      │──▶│ Framework          │──▶│ npx playwright│
  │ (TypeScript)    │   │ Architect          │   │ test          │
  └─────────────────┘   └────────────────────┘   └──────────────┘
        │                        │
        ▼                        ▼
  page-objects/            package.json, tsconfig.json,
  SwagLabsLoginPage.ts     playwright.config.ts, tests/*.spec.ts
```

All agents use a single LLM from OpenRouter, configured in `.env`.

## Folder structure

```
crewai_demo/
├── app.py                 # FastAPI backend (two-phase orchestration + SSE streams)
├── agents.py              # CrewAI agents + tasks (roles, goals, prompts)
├── page_reader.py         # Agent 1: Playwright page snapshot
├── framework_writer.py    # Saves agent file output to output/<run_id>/
├── test_runner.py         # Runs npx playwright test
├── static/                # Legacy simple UI (still served at :8000)
├── frontend/              # Next.js 15 + Tailwind 4 UI (beige theme, live pipeline)
│   └── src/
│       ├── app/           # layout.tsx, page.tsx, globals.css
│       └── lib/api.ts     # SSE client + types
├── output/<run_id>/       # Generated Playwright framework per run
├── runs/<run_id>/         # Raw per-step agent outputs
├── requirements.txt
├── .env.example           # Copy to .env and fill in
└── .gitignore
```

## Setup

Requires **Python 3.10+** and **Node.js 18+** (Playwright tests run on Node).

```bash
cd crewai_demo

# 1. Python environment + dependencies
python -m venv .venv
.venv\Scripts\activate            # Windows
# source .venv/bin/activate       # macOS / Linux

pip install -r requirements.txt

# 2. Browser engine for the page reader (Agent 1)
python -m playwright install chromium

# 3. Configuration
copy .env.example .env            # Windows
# cp .env.example .env            # macOS / Linux
```

Open `.env` and set your OpenRouter key (free at https://openrouter.ai/keys):

```env
OPENROUTER_API_KEY=sk-or-v1-...
OPENROUTER_MODEL=minimax/minimax-m3:free
```

> The model is fully configurable. `minimax/minimax-m3:free` is verified
> working and gives clean output. Swap in `anthropic/claude-3.5-sonnet`,
> `openai/gpt-4o-mini`, or any model OpenRouter exposes for better code
> quality — free models occasionally emit minor TS syntax errors.

## Run

Two processes — the backend API and the Next.js UI.

**Terminal 1 — backend (port 8000):**

```bash
cd crewai_demo
.venv\Scripts\python app.py        # Windows
# .venv/bin/python app.py          # macOS / Linux
```

**Terminal 2 — frontend (port 3000):**

```bash
cd crewai_demo/frontend
npm install                        # first time only
npm run dev
```

Open **http://localhost:3000** (the old static UI still exists at
http://127.0.0.1:8000 if you ever want it).

Then, in the UI:

1. Enter a URL (e.g. `https://saucedemo.com`), optionally add requirements,
   click **Generate Test Cases** — watch the live pipeline panel show
   Agent 1 (Page Reader) and Agent 2 (Test Case Designer) working in real
   time.
2. Review the generated test cases with priorities, steps and expected
   results. Tick the ones you want.
3. Click **Automate Selected** — the pipeline panel now shows Agent 3
   (POM Writer) and Agent 4 (Framework Architect), then the generated POM
   and framework files appear under Results (`output/<run_id>/` on disk).
4. Open the **Test Run** tab and click **Run Tests** — commands stream in
   live (`npm install --include=dev`, `npx playwright test`) and the
   output shows inline.

The frontend talks to the backend at `NEXT_PUBLIC_API_BASE`
(default `http://127.0.0.1:8000`) — override with
`frontend/.env.local` if you change ports.

## Agents

| Agent | Role | Task |
| --- | --- | --- |
| Page Reader | Browser automation | Opens the URL with Playwright, extracts structure (title, inputs, buttons, headings, links, text) |
| Test Case Designer | Senior Test Case Designer | Turns the page snapshot into 8-12 P0-P3 test cases (JSON: id, title, priority, preconditions, steps, expected) |
| POM Writer | Senior Automation Engineer (Page Objects) | Writes a typed TypeScript Page Object Model with locators + action methods |
| Framework Architect | Senior Test Framework Architect | Writes package.json, tsconfig, playwright.config.ts and a spec file covering the selected cases |

Roles, goals and backstories live in `agents.py` (configurable via `.env`
overrides) and every agent consumes the same `.env`-configured LLM.

## .env reference

| Variable | Default | Purpose |
| --- | --- | --- |
| `OPENROUTER_API_KEY` | — | Your OpenRouter key (**required**) |
| `OPENROUTER_MODEL` | `minimax/minimax-m3:free` | LLM used by all agents |
| `OPENROUTER_TEMPERATURE` | `0.5` | LLM temperature |
| `OPENROUTER_MAX_TOKENS` | `8000` | Max output tokens per call |
| `OPENROUTER_TIMEOUT_MS` | `180000` | LLM call timeout |
| `PAGE_READ_TIMEOUT_MS` | `30000` | Page load timeout (ms) |
| `PAGE_READ_HEADLESS` | `true` | `false` to watch the browser |
| `PAGE_READ_SLIM` | `true` | Trim page text to keep tokens low |
| `PAGE_READ_MAX_LINKS` | `40` | Max links captured |
| `TEST_RUNNER_AUTO_INSTALL` | `true` | `npm install --include=dev` before tests |
| `TEST_RUNNER_BROWSER_INSTALL` | `false` | `npx playwright install chromium` before tests |
| `AGENT_VERBOSE` / `CREW_VERBOSE` | `false` | CrewAI logging |
| `HOST` / `PORT` | `127.0.0.1` / `8000` | Server bind address |

## Notes

- `.env` and `output/` are gitignored — your API key never gets committed.
- If your npm has `omit=dev` globally, the test runner forces `--include=dev`
  so `@playwright/test` installs correctly.
- The free OpenRouter model is fine for demos. Use a paid model for
  production-grade frameworks.
