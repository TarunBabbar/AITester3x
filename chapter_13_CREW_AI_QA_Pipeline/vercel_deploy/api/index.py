"""Vercel Flask entry point for the Jira QA Crew pipeline.

Serves a small single-page UI and a JSON API that runs the real four-agent
CrewAI pipeline synchronously inside the Vercel Python function. The pure
``src/jira_qa_crew`` package is imported via sys.path (same trick as app.py);
Streamlit and its UI modules are never imported here.

Env vars (set in the Vercel project):
    LLM_MODEL, LLM_API_KEY, LLM_BASE_URL, LLM_TEMPERATURE, LLM_MAX_TOKENS,
    JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_INTEGRATION_MODE=rest,
    OUTPUT_DIR, JIRA_QA_CREW_SKIP_DOTENV=1, LLM_STRUCTURED_OUTPUT
"""

from __future__ import annotations

import base64
import logging
import os
import sys
from pathlib import Path

from flask import Flask, jsonify, render_template, request

# Find the src/ directory holding jira_qa_crew. It is installed as a package
# from vercel_deploy/src (pyproject.toml [tool.setuptools.packages.find]
# where=["src"]); fall back to a sys.path walk for local dev.
try:
    import jira_qa_crew  # noqa: F401
except ImportError:
    _here = Path(__file__).resolve()
    for _p in (_here.parent.parent, *_here.parent.parent.parents):
        if (_p / "src" / "jira_qa_crew").is_dir():
            sys.path.insert(0, str(_p / "src"))
            break

from jira_qa_crew.config import Settings  # noqa: E402
from jira_qa_crew.exceptions import TicketInputError  # noqa: E402
from jira_qa_crew.services import artifacts as artifacts_service  # noqa: E402
from jira_qa_crew.services.pipeline import QAPipeline, new_run_id  # noqa: E402
from jira_qa_crew.services.tickets import parse_ticket_input  # noqa: E402

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))

app = Flask(__name__)

#: Hard cap per request. One ticket is 4 sequential LLM calls (3-6 min).
MAX_TICKETS_PER_REQUEST = 1


# ---------------------------------------------------------------------------
# UI (served by Flask from api/templates) and liveness
# ---------------------------------------------------------------------------
@app.get("/")
def index():
    return render_template("index.html")


@app.get("/health")
def health():
    return jsonify({"ok": True})


# ---------------------------------------------------------------------------
# API
# ---------------------------------------------------------------------------
def _settings() -> Settings:
    return Settings.load(env_file=os.devnull)


@app.post("/api/run")
def run_pipeline():
    settings = _settings()

    body = request.get_json(silent=True) or {}
    raw_tickets = body.get("tickets") or body.get("ticket") or ""
    if isinstance(raw_tickets, str):
        raw_tickets = [raw_tickets]

    try:
        parsed = parse_ticket_input(
            "\n".join(str(t) for t in raw_tickets),
            key_pattern=settings.jira_key_pattern,
            max_tickets=MAX_TICKETS_PER_REQUEST,
            max_chars=settings.pipeline_max_input_chars,
        )
    except TicketInputError as exc:
        return jsonify({"error": settings.redact(str(exc))}), 400

    if not parsed.has_valid:
        return jsonify({"error": "No valid Jira ticket IDs were found in the input."}), 400

    if settings.demo_mode is False:
        problems = settings.blocking_problems()
        if problems:
            return jsonify({"error": " ; ".join(problems)}), 400

    tickets = parsed.valid[:MAX_TICKETS_PER_REQUEST]

    pipeline = QAPipeline(settings, progress=None)
    run = pipeline.run(
        tickets,
        mode=None,
        invalid_inputs=parsed.invalid,
        duplicates=parsed.duplicates,
        run_id=new_run_id(),
    )

    payload = _summarize_run(settings, run)
    return jsonify(payload)


def _summarize_run(settings: Settings, run) -> dict:
    """Shape the RunSummary into a JSON-safe response with rendered artifacts."""
    results = []
    for result in run.results:
        ticket = {
            "ticket_key": result.ticket_key,
            "status": result.status.value,
            "source": result.source.value if result.source else None,
            "error": settings.redact(result.error) if result.error else "",
            "warnings": [settings.redact(w) for w in result.warnings],
            "duration_seconds": result.duration_seconds,
            "analysis": (
                result.analysis.model_dump(mode="json") if result.analysis else None
            ),
            "test_plan": result.test_plan.model_dump(mode="json") if result.test_plan else None,
            "test_cases": (
                result.test_cases.model_dump(mode="json") if result.test_cases else None
            ),
            "playwright": (
                result.playwright.model_dump(mode="json") if result.playwright else None
            ),
            "coverage": result.coverage.model_dump(mode="json") if result.coverage else None,
        }

        # Rendered artifacts, built in memory (no disk dependency).
        md: dict[str, str] = {}
        if result.analysis:
            md["requirements_analysis.md"] = artifacts_service.render_requirements_md(
                result.analysis, result.issue
            )
        if result.test_plan:
            md["test_plan.md"] = artifacts_service.render_test_plan_md(result.test_plan)
        if result.test_cases:
            md["test_cases.md"] = artifacts_service.render_test_cases_md(result.test_cases)
            md["test_cases.csv"] = artifacts_service.render_test_cases_csv(result.test_cases)
        if result.playwright:
            md["playwright_tests.md"] = artifacts_service.render_playwright_md(result.playwright)
        if result.coverage:
            md["traceability_matrix.csv"] = artifacts_service.render_traceability_csv(
                result.coverage
            )
        ticket["artifacts_md"] = md
        results.append(ticket)

    payload = {
        "run_id": run.run_id,
        "requested_keys": run.requested_keys,
        "invalid_inputs": run.invalid_inputs,
        "duplicates_removed": run.duplicates_removed,
        "successful": run.successful,
        "results": results,
    }

    try:
        zip_bytes = artifacts_service.build_zip(run)
        payload["zip_b64"] = base64.b64encode(zip_bytes).decode("ascii")
    except Exception:  # noqa: BLE001 - a missing artifact must not kill the response
        app.logger.warning("could not build artifact zip", exc_info=True)
        payload["zip_b64"] = None

    return payload


# ---------------------------------------------------------------------------
# Vercel Python functions call `app` by default for Flask.
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=False)
