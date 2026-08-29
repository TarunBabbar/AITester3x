"""
CrewAI QA Demo — FastAPI Backend

Two-phase workflow:

  PHASE 1 — Generate Tests
    URL -> [Agent 1: Page Reader] -> page snapshot
        -> [Agent 2: Test Case Designer] -> structured test cases (JSON)

  PHASE 2 — Automate Selected
    selected test case IDs -> [Agent 3: POM Writer] -> TypeScript POM
                           -> [Agent 4: Framework Architect] -> full
                              Playwright + TS framework (proper structure)
                           -> [Test Runner] -> npx playwright test

The UI (static/) drives both phases. All configuration lives in .env.

Run:
    .venv\\Scripts\\python app.py
"""

import json
import logging
import os
import re
import time
import uuid
from pathlib import Path
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from agents import (
    run_framework_agent,
    run_pom_writer_agent,
    run_test_case_agent,
)
from framework_writer import OUTPUT_DIR, write_framework
from page_reader import PageReader
from test_runner import TestRunner, node_available, npm_available

load_dotenv()

# ---------------------------------------------------------------------------
# Logging — everything the pipeline does shows up in the terminal.
# LOG_LEVEL comes from .env (DEBUG | INFO | WARNING | ERROR).
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("app")

BASE_DIR = Path(__file__).resolve().parent
STATIC_DIR = BASE_DIR / "static"
RUNS_DIR = BASE_DIR / "runs"

app = FastAPI(title="CrewAI QA Demo", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------
class GenerateRequest(BaseModel):
    url: str
    requirements: str = ""


class AutomateRequest(BaseModel):
    run_id: str
    selected: list[str]


class RunTestsRequest(BaseModel):
    run_id: str


# ---------------------------------------------------------------------------
# Lifecycle
# ---------------------------------------------------------------------------
@app.on_event("startup")
def _startup() -> None:
    RUNS_DIR.mkdir(parents=True, exist_ok=True)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def _save_run(run_id: str, step: str, content: str) -> Path:
    """Persist one pipeline step's output to runs/<run_id>/."""
    step_dir = RUNS_DIR / run_id
    step_dir.mkdir(parents=True, exist_ok=True)
    safe = step.lower().replace(" ", "_").replace("/", "_")
    path = step_dir / f"{safe}.md"
    path.write_text(content, encoding="utf-8")
    return path


def _extract_json(text: str) -> list:
    """Best-effort parse of a JSON array from an LLM response."""
    text = text.strip()
    # Strip markdown fences if the model wrapped them anyway
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text, flags=re.DOTALL)
    try:
        data = json.loads(text)
        if isinstance(data, list):
            return data
    except json.JSONDecodeError:
        pass
    # Fallback: try to find the first [...] block
    match = re.search(r"\[[\s\S]*\]", text)
    if match:
        try:
            data = json.loads(match.group(0))
            if isinstance(data, list):
                return data
        except json.JSONDecodeError:
            pass
    return []


def _extract_code_block(text: str) -> str:
    """Strip ```...``` fences and surrounding prose from an LLM code reply."""
    text = text.strip()
    match = re.search(r"```\w*\s*\n([\s\S]*?)```", text)
    if match:
        return match.group(1).strip()
    # No fence — assume the whole reply is code
    return text


def _pom_class_name(pom_code: str) -> str:
    """Best-effort class name from the POM code for the filename."""
    match = re.search(r"class\s+(\w+)", pom_code)
    return (match.group(1) if match else "PageObject") + ".ts"


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------
@app.get("/api/health")
def health() -> dict:
    node_ok, node_msg = node_available()
    return {
        "status": "ok",
        "node": {"available": node_ok, "detail": node_msg},
        "npm": {"available": npm_available()},
    }


@app.post("/api/generate-tests")
def generate_tests(req: GenerateRequest) -> dict:
    """Phase 1: read the URL and generate structured test cases."""
    run_id = uuid.uuid4().hex[:8]
    steps: list[dict] = []
    t0 = time.perf_counter()
    log.info("=" * 60)
    log.info("PHASE 1 | run=%s | url=%s | requirements=%r",
             run_id, req.url, req.requirements[:80] or "-")

    try:
        # --- Step 1: Page Reader (Agent 1) -------------------------------
        reader = PageReader()
        snapshot = reader.snapshot(req.url)
        page_snapshot = "\n".join(f"{k}: {v}" for k, v in snapshot.items())
        _save_run(run_id, "01_page_snapshot", page_snapshot)
        steps.append({
            "step": "Page Reader",
            "status": "done",
            "detail": f"Read '{snapshot.get('title', 'untitled page')}' — "
                      f"{len(snapshot.get('inputs', []))} inputs, "
                      f"{len(snapshot.get('buttons', []))} buttons",
        })

        # --- Step 2: Test Case Designer (Agent 2) -------------------------
        raw = run_test_case_agent(page_snapshot, req.requirements)
        _save_run(run_id, "02_test_cases_raw", raw)
        test_cases = _extract_json(raw)

        if not test_cases:
            log.warning("run=%s | Test Case Designer returned no parseable JSON", run_id)
            steps.append({
                "step": "Test Case Designer",
                "status": "error",
                "detail": "Model did not return valid JSON. Showing raw output.",
            })
            return {
                "run_id": run_id,
                "status": "error",
                "steps": steps,
                "test_cases": [],
                "raw": raw,
            }

        steps.append({
            "step": "Test Case Designer",
            "status": "done",
            "detail": f"Generated {len(test_cases)} test cases "
                      f"({sum(1 for t in test_cases if t.get('priority') == 'P0')} P0)",
        })

        log.info("PHASE 1 DONE | run=%s | %d test cases | %.1fs total",
                 run_id, len(test_cases), time.perf_counter() - t0)
        return {
            "run_id": run_id,
            "status": "success",
            "steps": steps,
            "test_cases": test_cases,
            "url": req.url,
        }

    except Exception as exc:
        log.exception("PHASE 1 FAILED | run=%s | %s", run_id, exc)
        steps.append({"step": "Pipeline", "status": "error", "detail": str(exc)})
        return {
            "run_id": run_id,
            "status": "error",
            "steps": steps,
            "test_cases": [],
        }


@app.post("/api/automate")
def automate(req: AutomateRequest) -> dict:
    """Phase 2a: generate POM + Playwright framework for selected test cases."""
    run_dir = RUNS_DIR / req.run_id
    steps: list[dict] = []

    if not run_dir.exists():
        return {"run_id": req.run_id, "status": "error",
                "steps": [{"step": "Automate", "status": "error",
                           "detail": "Run not found. Generate tests first."}]}

    page_snapshot = (run_dir / "01_page_snapshot.md").read_text(encoding="utf-8", errors="replace")
    raw_cases = (run_dir / "02_test_cases_raw.md").read_text(encoding="utf-8", errors="replace")
    all_cases = _extract_json(raw_cases)

    # Keep only the selected test cases
    selected_ids = set(req.selected)
    selected = [tc for tc in all_cases if tc.get("id") in selected_ids]
    if not selected:
        return {"run_id": req.run_id, "status": "error",
                "steps": [{"step": "Automate", "status": "error",
                           "detail": "No matching selected test cases."}]}

    selected_text = json.dumps(selected, indent=2)
    t0 = time.perf_counter()
    log.info("PHASE 2 | run=%s | automating %d cases: %s",
             req.run_id, len(selected), ", ".join(tc.get("id", "?") for tc in selected))

    try:
        # --- Step 3: POM Writer (Agent 3) ---------------------------------
        pom_code = run_pom_writer_agent(page_snapshot, selected_text)
        _save_run(req.run_id, "03_page_object_model", pom_code)

        # Save the POM into the run's framework so the spec import resolves
        clean_pom = _extract_code_block(pom_code)
        pom_filename = _pom_class_name(clean_pom)
        output_run_dir = OUTPUT_DIR / req.run_id
        (output_run_dir / "page-objects").mkdir(parents=True, exist_ok=True)
        (output_run_dir / "page-objects" / pom_filename).write_text(
            clean_pom, encoding="utf-8"
        )
        log.info("run=%s | POM saved -> output/%s/page-objects/%s",
                 req.run_id, req.run_id, pom_filename)

        steps.append({
            "step": "POM Writer",
            "status": "done",
            "detail": f"Generated {len(pom_code.splitlines())} lines of TypeScript "
                      f"(page-objects/{pom_filename})",
        })

        # --- Step 4: Framework Architect (Agent 4) -------------------------
        framework_text = run_framework_agent(page_snapshot, selected_text, pom_code)
        _save_run(req.run_id, "04_framework_raw", framework_text)
        files = write_framework(framework_text, run_id=req.run_id)
        steps.append({
            "step": "Framework Architect",
            "status": "done",
            "detail": f"Wrote {len(files['written'])} files: "
                      f"{', '.join(files['written'])}",
        })

        log.info("PHASE 2 DONE | run=%s | files=%s | %.1fs total",
                 req.run_id, files["written"] + [f"page-objects/{pom_filename}"],
                 time.perf_counter() - t0)

        return {
            "run_id": req.run_id,
            "status": "success",
            "steps": steps,
            "pom_code": pom_code,
            "framework_files": files.get("written", []) + [f"page-objects/{pom_filename}"],
            "output_dir": files.get("output_dir", ""),
            "selected": [tc.get("id") for tc in selected],
        }

    except Exception as exc:
        log.exception("PHASE 2 FAILED | run=%s | %s", req.run_id, exc)
        steps.append({"step": "Automate", "status": "error", "detail": str(exc)})
        return {"run_id": req.run_id, "status": "error", "steps": steps}


@app.post("/api/run-tests")
def run_tests(req: RunTestsRequest) -> dict:
    """Phase 2b: execute the generated Playwright framework for a run."""
    steps: list[dict] = []
    run_dir = OUTPUT_DIR / req.run_id
    if not (run_dir / "package.json").exists():
        log.warning("run=%s | run-tests skipped: no generated framework", req.run_id)
        steps.append({"step": "Test Runner", "status": "error",
                      "detail": "No generated framework for this run. Automate test cases first."})
        return {"run_id": req.run_id, "status": "error", "steps": steps,
                "test_result": {"success": None, "output": ""}}
    log.info("PHASE 2b | run=%s | running generated tests in output/%s",
             req.run_id, req.run_id)
    t0 = time.perf_counter()
    try:
        result = TestRunner(output_dir=run_dir).run()
        log.info("run=%s | test run %s | %.1fs",
                 req.run_id,
                 "PASSED" if result["success"] else "FAILED",
                 time.perf_counter() - t0)
        steps.append({
            "step": "Test Runner",
            "status": "done",
            "detail": "passed" if result["success"] else "failed",
        })
        return {
            "run_id": req.run_id,
            "status": "success",
            "steps": steps,
            "test_result": result,
        }
    except Exception as exc:
        log.exception("PHASE 2b FAILED | run=%s | %s", req.run_id, exc)
        steps.append({"step": "Test Runner", "status": "error", "detail": str(exc)})
        return {"run_id": req.run_id, "status": "error", "steps": steps,
                "test_result": {"success": None, "output": ""}}


@app.get("/api/run/{run_id}/files")
def run_files(run_id: str) -> dict:
    """List the saved pipeline outputs for a run."""
    step_dir = RUNS_DIR / run_id
    if not step_dir.exists():
        return {"run_id": run_id, "files": []}
    files = [
        {"name": p.name, "content": p.read_text(encoding="utf-8", errors="replace")}
        for p in sorted(step_dir.glob("*.md"))
    ]
    return {"run_id": run_id, "files": files}


@app.get("/api/output")
def list_output() -> dict:
    """List generated framework files on disk (from the output dir)."""
    if not OUTPUT_DIR.exists():
        return {"files": []}
    files = []
    for p in sorted(OUTPUT_DIR.rglob("*")):
        if p.is_file() and "logs" not in p.parts and "__pycache__" not in p.parts \
                and "node_modules" not in p.parts:
            files.append({"name": str(p.relative_to(OUTPUT_DIR)),
                          "content": p.read_text(encoding="utf-8", errors="replace")})
    return {"files": files}


# ---------------------------------------------------------------------------
# Static UI
# ---------------------------------------------------------------------------
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


@app.get("/")
def index() -> FileResponse:
    return FileResponse(STATIC_DIR / "index.html")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app:app",
        host=os.getenv("HOST", "127.0.0.1"),
        port=int(os.getenv("PORT", "8000")),
        reload=False,
    )
