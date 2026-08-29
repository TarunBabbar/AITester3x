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
import subprocess
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
    run_ri_agent,
    run_test_case_agent,
)
from evaluator import (
    evaluate_automation,
    evaluate_ri_coverage,
    evaluate_test_cases,
    release_confidence,
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

# ---------------------------------------------------------------------------
# Run registry — tracks running pipelines so a client can stop them.
# active_runs: {run_id: threading.Event} — set when a run should stop.
# ---------------------------------------------------------------------------
import threading  # noqa: E402

_active_runs: dict[str, threading.Event] = {}
_active_runs_lock = threading.Lock()


def _register_run(run_id: str) -> threading.Event:
    """Register a run and return its stop event."""
    stop_event = threading.Event()
    with _active_runs_lock:
        _active_runs[run_id] = stop_event
    return stop_event


def _unregister_run(run_id: str) -> None:
    with _active_runs_lock:
        _active_runs.pop(run_id, None)


def _run_stopped(stop_event: threading.Event) -> bool:
    """True when the client asked to stop this run."""
    return stop_event.is_set()


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
    test_names: list[str] = []  # empty = run the whole suite


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
    """Best-effort parse of a JSON array from an LLM response.

    Handles markdown fences, surrounding prose, and TRUNCATED responses
    (free models frequently cut off mid-array). For truncation we repair
    the trailing unterminated string/array so the valid prefix still parses.
    """
    text = text.strip()
    # Strip markdown fences if the model wrapped them anyway
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text, flags=re.DOTALL).strip()

    # 1) Direct parse
    try:
        data = json.loads(text)
        if isinstance(data, list):
            return data
    except json.JSONDecodeError:
        pass

    # 2) Find the first [...] block
    match = re.search(r"\[[\s\S]*\]", text)
    if match:
        block = match.group(0)
        try:
            data = json.loads(block)
            if isinstance(data, list):
                return data
        except json.JSONDecodeError:
            pass
        # 3) Repair truncated trailing content: cut at the last `}` boundary
        #    of the longest valid JSON prefix, closing the array if needed.
        repaired = _repair_truncated_json(block)
        if repaired is not None:
            return repaired

    # 4) No closing bracket (heavily truncated) — repair the whole text
    repaired = _repair_truncated_json(text)
    if repaired is not None:
        return repaired

    return []


def _repair_truncated_json(block: str):
    """Best-effort repair of a truncated JSON array.

    Strategy: walk back from the end to the last complete object `}`; try
    closing the array there and parsing. If the truncated tail is a partial
    next element (common with free models), the prefix up to the last `}`
    is the maximum recoverable content. Returns the parsed list or None.
    """
    block = block.strip()
    if not block.startswith("["):
        return None

    for end in range(len(block) - 1, 0, -1):
        if block[end] == "}":
            candidate = block[: end + 1]
            try:
                data = json.loads(candidate + "]")
                if isinstance(data, list):
                    return data
            except json.JSONDecodeError:
                continue
    return None


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


def docker_available() -> bool:
    """True if the Docker engine is reachable (docker info succeeds)."""
    try:
        result = subprocess.run(
            ["docker", "info"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        return result.returncode == 0
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False


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
        "docker": {"available": docker_available()},
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
# SSE streaming endpoints — the Next.js UI consumes these to show live
# agent activity ("what is running and what it is doing right now").
#
# Each endpoint yields `data: {json}\n\n` events:
#   run_started | agent_started | agent_activity | agent_done
#   agent_failed | command_started | command_done | phase_complete | error
# ---------------------------------------------------------------------------
from fastapi.responses import StreamingResponse  # noqa: E402


def _sse(event: dict) -> str:
    return f"data: {json.dumps(event)}\n\n"


def _ms(t0: float) -> int:
    return int((time.perf_counter() - t0) * 1000)


@app.get("/api/config")
def config() -> dict:
    """Non-secret config the UI header displays."""
    from agents import _active_provider

    provider, model, _, _ = _active_provider()
    return {
        "provider": provider,
        "model": model,
        "headless": os.getenv("PAGE_READ_HEADLESS", "true"),
    }


@app.post("/api/stop/{run_id}")
def stop_run(run_id: str) -> dict:
    """Ask the backend to stop a running pipeline. The stream endpoint
    checks the stop flag between steps and shuts down cleanly."""
    with _active_runs_lock:
        stop_event = _active_runs.get(run_id)
    if stop_event is None:
        return {"run_id": run_id, "stopped": False,
                "message": "No active run with that id."}
    stop_event.set()
    log.info("run=%s | stop requested", run_id)
    return {"run_id": run_id, "stopped": True}


@app.post("/api/generate-tests/stream")
def generate_tests_stream(req: GenerateRequest) -> StreamingResponse:
    """Phase 1 as a live event stream (Page Reader + Test Case Designer)."""
    def events():
        run_id = uuid.uuid4().hex[:8]
        stop_event = _register_run(run_id)
        yield _sse({"type": "run_started", "runId": run_id, "phase": 1})
        log.info("PHASE 1 | run=%s | url=%s", run_id, req.url)
        try:
            # --- Step: Requirement Intelligence (Agent 0) — first stage -----
            # RI works from the URL + user requirements alone; the page
            # snapshot (next step) is added later for test case design.
            yield _sse({
                "type": "agent_started", "key": "ri",
                "title": "Requirement Intelligence",
                "activity": "Converting the URL + requirements into structured intelligence...",
            })
            t0 = time.perf_counter()
            ri_text = run_ri_agent(req.url, req.requirements)
            _save_run(run_id, "02_requirement_intelligence", ri_text)
            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return
            yield _sse({
                "type": "agent_done", "key": "ri",
                "detail": f"RI produced ({len(ri_text.splitlines())} lines)",
                "output": ri_text,
                "durationMs": _ms(t0),
            })

            # --- Step: DeepEval coverage of RI vs user input ----------------
            # Evaluates the RI immediately, before the browser read, so a
            # weak RI is caught early.
            yield _sse({
                "type": "eval_started", "key": "coverage_eval",
                "title": "Coverage Evaluation · DeepEval",
                "activity": "Cross-verifying the RI against the user's requirements...",
            })
            t0 = time.perf_counter()
            try:
                eval_result = evaluate_ri_coverage(
                    (req.requirements or "Derive requirements from the page snapshot."),
                    ri_text,
                )
                eval_result["durationMs"] = _ms(t0)
                (RUNS_DIR / run_id / "eval_coverage_eval.json").write_text(
                    json.dumps(eval_result), encoding="utf-8")
                yield _sse({"type": "eval_done", "key": "coverage_eval", "result": eval_result})
            except Exception as exc:
                log.exception("run=%s | coverage eval failed: %s", run_id, exc)
                yield _sse({
                    "type": "eval_failed", "key": "coverage_eval",
                    "detail": str(exc),
                })

            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return

            # --- Test Case Generation: read the page, then design cases -----
            # Reading the URL is part of generating the test cases — it gives
            # the designer concrete element structure to target.
            yield _sse({
                "type": "agent_started", "key": "page_reader",
                "title": "Page Reader",
                "activity": "Launching headless Chromium and loading the page for test case design...",
            })
            t0 = time.perf_counter()
            snapshot = PageReader().snapshot(req.url)
            page_snapshot = "\n".join(f"{k}: {v}" for k, v in snapshot.items())
            _save_run(run_id, "01_page_snapshot", page_snapshot)
            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return
            yield _sse({
                "type": "agent_done", "key": "page_reader",
                "detail": f"Read '{snapshot.get('title', 'untitled page')}' — "
                          f"{len(snapshot.get('inputs', []))} inputs, "
                          f"{len(snapshot.get('buttons', []))} buttons, "
                          f"{len(snapshot.get('links', []))} links",
                "durationMs": _ms(t0),
            })

            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return
            yield _sse({
                "type": "agent_started", "key": "test_designer",
                "title": "Test Case Designer",
                "activity": "Analysing the requirement intelligence + page and drafting prioritised test cases...",
            })
            t0 = time.perf_counter()
            # Test designer gets RI + the live page snapshot for accuracy
            raw = run_test_case_agent(ri_text + "\n\n### Page Snapshot\n" + page_snapshot)
            _save_run(run_id, "03_test_cases_raw", raw)
            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return
            test_cases = _extract_json(raw)

            if not test_cases:
                log.warning("run=%s | no parseable JSON from Test Case Designer", run_id)
                yield _sse({
                    "type": "agent_failed", "key": "test_designer",
                    "detail": "Model did not return valid JSON test cases.",
                })
                yield _sse({"type": "error", "message": "Model returned no parseable JSON."})
                return

            yield _sse({
                "type": "agent_done", "key": "test_designer",
                "detail": f"Generated {len(test_cases)} test cases "
                          f"({sum(1 for t in test_cases if t.get('priority') == 'P0')} P0)",
                "durationMs": _ms(t0),
            })

            # --- Step: DeepEval coverage of test cases vs RI ---------------
            yield _sse({
                "type": "eval_started", "key": "cases_eval",
                "title": "Test Case Coverage · DeepEval",
                "activity": "Cross-verifying the generated test cases against the RI...",
            })
            t0 = time.perf_counter()
            try:
                eval_result = evaluate_test_cases(
                    ri_text,
                    json.dumps(test_cases, indent=2),
                )
                eval_result["durationMs"] = _ms(t0)
                (RUNS_DIR / run_id / "eval_cases_eval.json").write_text(
                    json.dumps(eval_result), encoding="utf-8")
                yield _sse({"type": "eval_done", "key": "cases_eval", "result": eval_result})
            except Exception as exc:
                log.exception("run=%s | cases eval failed: %s", run_id, exc)
                yield _sse({"type": "eval_failed", "key": "cases_eval", "detail": str(exc)})

            yield _sse({
                "type": "phase_complete", "phase": 1,
                "payload": {
                    "run_id": run_id,
                    "test_cases": test_cases,
                    "url": req.url,
                    "ri_text": ri_text,
                },
            })
            log.info("PHASE 1 DONE | run=%s | %d cases | %.1fs",
                     run_id, len(test_cases), time.perf_counter())
        except Exception as exc:
            log.exception("PHASE 1 FAILED | run=%s | %s", run_id, exc)
            yield _sse({"type": "error", "message": str(exc)})
        finally:
            _unregister_run(run_id)

    return StreamingResponse(events(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache",
                                      "X-Accel-Buffering": "no"})


@app.post("/api/automate/stream")
def automate_stream(req: AutomateRequest) -> StreamingResponse:
    """Phase 2a as a live event stream (POM Writer + Framework Architect)."""
    def events():
        run_id = req.run_id
        stop_event = _register_run(run_id)
        yield _sse({"type": "run_started", "runId": run_id, "phase": 2})
        log.info("PHASE 2 | run=%s | selected=%s", run_id, req.selected)
        try:
            run_dir = RUNS_DIR / run_id
            page_snapshot = (run_dir / "01_page_snapshot.md").read_text(encoding="utf-8", errors="replace")
            ri_text = ""
            ri_path = run_dir / "02_requirement_intelligence.md"
            if ri_path.exists():
                ri_text = ri_path.read_text(encoding="utf-8", errors="replace")
            all_cases = _extract_json(
                (run_dir / "03_test_cases_raw.md").read_text(encoding="utf-8", errors="replace"))
            selected = [tc for tc in all_cases if tc.get("id") in set(req.selected)]
            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return
            if not selected:
                yield _sse({"type": "error", "message": "No matching selected test cases."})
                return
            selected_text = json.dumps(selected, indent=2)

            yield _sse({
                "type": "agent_started", "key": "pom_writer",
                "title": "POM Writer",
                "activity": "Writing a typed TypeScript Page Object Model for the page...",
            })
            t0 = time.perf_counter()
            pom_code = run_pom_writer_agent(page_snapshot, selected_text)
            _save_run(run_id, "03_page_object_model", pom_code)
            clean_pom = _extract_code_block(pom_code)
            pom_filename = _pom_class_name(clean_pom)
            (OUTPUT_DIR / run_id / "page-objects").mkdir(parents=True, exist_ok=True)
            (OUTPUT_DIR / run_id / "page-objects" / pom_filename).write_text(
                clean_pom, encoding="utf-8")
            yield _sse({
                "type": "agent_done", "key": "pom_writer",
                "detail": f"page-objects/{pom_filename} "
                          f"({len(clean_pom.splitlines())} lines)",
                "durationMs": _ms(t0),
            })

            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return

            yield _sse({
                "type": "agent_started", "key": "framework_architect",
                "title": "Framework Architect",
                "activity": "Generating package.json, configs and the Playwright spec for the selected cases...",
            })
            t0 = time.perf_counter()
            framework_text = run_framework_agent(page_snapshot, selected_text, pom_code)
            _save_run(run_id, "04_framework_raw", framework_text)
            if _run_stopped(stop_event):
                yield _sse({"type": "run_stopped", "runId": run_id})
                return
            files = write_framework(framework_text, run_id=run_id)
            all_files = files["written"] + [f"page-objects/{pom_filename}"]
            yield _sse({
                "type": "agent_done", "key": "framework_architect",
                "detail": f"Wrote {len(all_files)} files: {', '.join(all_files)}",
                "durationMs": _ms(t0),
            })

            # --- Step: DeepEval coverage of automation vs RI + tests --------
            yield _sse({
                "type": "eval_started", "key": "automation_eval",
                "title": "Automation Coverage · DeepEval",
                "activity": "Cross-verifying the generated framework covers the selected test cases...",
            })
            t0 = time.perf_counter()
            try:
                eval_result = evaluate_automation(
                    ri_text or page_snapshot,
                    f"Files written: {', '.join(all_files)}\n\n{framework_text[:4000]}",
                )
                eval_result["durationMs"] = _ms(t0)
                (RUNS_DIR / run_id / "eval_automation_eval.json").write_text(
                    json.dumps(eval_result), encoding="utf-8")
                yield _sse({"type": "eval_done", "key": "automation_eval", "result": eval_result})
            except Exception as exc:
                log.exception("run=%s | automation eval failed: %s", run_id, exc)
                yield _sse({"type": "eval_failed", "key": "automation_eval", "detail": str(exc)})

            yield _sse({
                "type": "phase_complete", "phase": 2,
                "payload": {
                    "run_id": run_id,
                    "pom_code": pom_code,
                    "framework_files": all_files,
                    "output_dir": files.get("output_dir", ""),
                    "selected": [tc.get("id") for tc in selected],
                },
            })
            log.info("PHASE 2 DONE | run=%s | files=%s | %.1fs",
                     run_id, all_files, time.perf_counter())
        except Exception as exc:
            log.exception("PHASE 2 FAILED | run=%s | %s", run_id, exc)
            yield _sse({"type": "error", "message": str(exc)})
        finally:
            _unregister_run(run_id)

    return StreamingResponse(events(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache",
                                      "X-Accel-Buffering": "no"})


@app.post("/api/run-tests/stream")
def run_tests_stream(req: RunTestsRequest) -> StreamingResponse:
    """Phase 2b as a live event stream (npm install + npx playwright test)."""
    def events():
        run_id = req.run_id
        stop_event = _register_run(run_id)
        yield _sse({"type": "run_started", "runId": run_id, "phase": 3})
        run_dir = OUTPUT_DIR / run_id
        if not (run_dir / "package.json").exists():
            yield _sse({"type": "error",
                        "message": "No generated framework for this run. Automate test cases first."})
            _unregister_run(run_id)
            return
        log.info("PHASE 2b | run=%s | running tests | selected=%d",
                 run_id, len(req.test_names))
        try:
            # --- Docker pre-flight: tests need the Docker engine running ----
            yield _sse({"type": "docker_check"})
            if not docker_available():
                yield _sse({
                    "type": "error",
                    "message": (
                        "Docker is not running. Start Docker Desktop on your "
                        "machine, wait for the engine, then run the tests again."
                    ),
                })
                log.warning("run=%s | docker not available — test run aborted", run_id)
                _unregister_run(run_id)
                return
            yield _sse({"type": "docker_ok"})

            lines = []
            ok = False
            for cmd in TestRunner(output_dir=run_dir)._commands(req.test_names):
                if _run_stopped(stop_event):
                    yield _sse({"type": "run_stopped", "runId": run_id})
                    return
                yield _sse({"type": "command_started", "command": cmd})
                t0 = time.perf_counter()
                result = subprocess.run(cmd, shell=True, cwd=run_dir,
                                        capture_output=True, text=True,
                                        timeout=int(os.getenv("TEST_RUNNER_TIMEOUT_MS", "600000")))
                combined = (result.stdout or "") + (result.stderr or "")
                lines.append(combined)
                yield _sse({"type": "command_done", "command": cmd,
                            "exitCode": result.returncode,
                            "durationMs": _ms(t0)})
                if result.returncode != 0:
                    break
            full_output = "\n".join(lines)
            ok = "passed" in full_output.lower() and "failed" not in full_output.lower()

            # Release confidence from all evaluations collected so far
            eval_results = []
            for name in ("coverage_eval", "cases_eval", "automation_eval"):
                p = run_dir / f"eval_{name}.json"
                if p.exists():
                    try:
                        eval_results.append(json.loads(p.read_text(encoding="utf-8")))
                    except json.JSONDecodeError:
                        pass
            confidence = release_confidence(eval_results)
            yield _sse({
                "type": "release_score",
                "score": confidence,
                "eval_count": len(eval_results),
                "test_success": ok,
            })
            yield _sse({
                "type": "phase_complete", "phase": 3,
                "payload": {"run_id": run_id, "success": ok,
                            "output": full_output[-6000:],
                            "release_score": confidence},
            })
            log.info("PHASE 2b DONE | run=%s | %s | release=%.2f",
                     run_id, "PASSED" if ok else "FAILED", confidence)
        except Exception as exc:
            log.exception("PHASE 2b FAILED | run=%s | %s", run_id, exc)
            yield _sse({"type": "error", "message": str(exc)})
        finally:
            _unregister_run(run_id)

    return StreamingResponse(events(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache",
                                      "X-Accel-Buffering": "no"})


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
