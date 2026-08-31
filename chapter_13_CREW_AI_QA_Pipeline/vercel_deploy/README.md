# Jira QA Crew — Vercel deployment

A minimal Flask app that runs the same four-agent CrewAI QA pipeline as the
Streamlit app, but inside a **Vercel Python function**. It reuses the pure
Python `../src/jira_qa_crew` package unchanged — no Streamlit, no UI framework.

```
vercel_deploy/
  api/index.py            Flask entry point (Vercel Python function)
  requirements.txt        Python deps (no streamlit/pandas)
  vercel.json             Function config: maxDuration 300s, 2GB memory
  static/index.html       Single-page UI (plain HTML/JS)
```

## How it works

- `POST /api/run` with `{"tickets": ["MDP-7"]}` runs the real pipeline
  synchronously (fetch → analysis → plan → cases → playwright) and returns the
  full structured result as JSON, plus every rendered artifact (markdown/CSV)
  and a base64 ZIP of the artifacts.
- `GET /` serves the UI; `GET /health` is a liveness check.
- The pipeline is capped at **1 ticket per request** — one ticket is four
  sequential LLM calls, roughly 3–6 minutes, which fits Vercel's 300s Hobby
  max duration. On Pro you can raise `maxDuration` in `vercel.json` (up to
  800s, or 1800s in the extended-duration beta) and bump
  `MAX_TICKETS_PER_REQUEST` in `api/index.py`.

## Deploy to Vercel

1. The code is already in the `TarunBabbar/AITester3x` repo under
   `chapter_13_CREW_AI_QA_Pipeline/vercel_deploy/`.
2. Vercel → **New Project** → import `TarunBabbar/AITester3x`.
3. **Root Directory**: `chapter_13_CREW_AI_QA_Pipeline/vercel_deploy`.
   Framework auto-detects **Flask**.
4. Project Settings → Environment Variables (these are required for a live run):

   ```
   LLM_MODEL=deepseek/deepseek-v4-flash
   LLM_API_KEY=<your Command Code API key>
   LLM_BASE_URL=https://api.commandcode.ai/provider/v1
   LLM_TEMPERATURE=0.1
   LLM_MAX_TOKENS=16000
   LLM_STRUCTURED_OUTPUT=auto

   JIRA_INTEGRATION_MODE=rest
   JIRA_URL=https://your-domain.atlassian.net
   JIRA_EMAIL=you@example.com
   JIRA_API_TOKEN=<your Jira API token>

   OUTPUT_DIR=/tmp/qa-outputs
   JIRA_QA_CREW_SKIP_DOTENV=1
   ```

   Nothing secret lives in the repo; all secrets come from Vercel env vars.

5. **Deploy**. Open the URL, enter a ticket ID (default `MDP-7`), and run.

## Local testing

```bash
cd chapter_13_CREW_AI_QA_Pipeline/vercel_deploy
python -m venv .venv && .venv\Scripts\activate    # Windows
pip install -r requirements.txt
set FLASK_APP=api/index.py
flask run --port 5000                              # http://localhost:5000
```

To test the pipeline against local fixtures without Jira (still calls the LLM,
so it costs tokens):

```bash
set DEMO_MODE=true
flask run --port 5000
# POST /api/run  {"tickets": ["VWO-48"]}
```

## Limitations

- **Ephemeral storage**: artifacts are returned in the response (and as a ZIP),
  not persisted. The `outputs/` dir on Vercel points at `/tmp`.
- **One ticket per request** on Hobby (300s function limit). Longer/multi-ticket
  runs need Pro or the async design (out of scope here).
- **Live Jira** requires the Jira REST env vars above; without them the run
  returns a clear 400 listing what's missing.
