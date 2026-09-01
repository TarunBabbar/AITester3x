"""Vercel Flask API surface tests.

These render the real Flask app. No Jira call and no LLM call happens by
default: the pipeline is stubbed so the routes, validation and JSON shaping
are exercised without spending tokens.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "vercel_deploy"))

from jira_qa_crew.config import Settings  # noqa: E402


@pytest.fixture(autouse=True)
def clean_env(monkeypatch, tmp_path):
    monkeypatch.setenv("JIRA_QA_CREW_SKIP_DOTENV", "1")
    for key in list(os.environ):
        if key.startswith(("JIRA_", "LLM_", "PIPELINE_", "DEMO_", "APP_", "OUTPUT_")):
            monkeypatch.delenv(key, raising=False)
    monkeypatch.setenv("OUTPUT_DIR", str(tmp_path / "outputs"))
    monkeypatch.setenv("LLM_MODEL", "deepseek/deepseek-v4-flash")
    monkeypatch.setenv("LLM_API_KEY", "sk-test-0123456789")
    monkeypatch.setenv("JIRA_URL", "https://example.atlassian.net")
    monkeypatch.setenv("JIRA_EMAIL", "qa@example.com")
    monkeypatch.setenv("JIRA_API_TOKEN", "token-0123456789")


@pytest.fixture
def client(monkeypatch):
    from api.index import app

    app.config["TESTING"] = True
    with app.test_client() as c:
        yield c


def _stub_pipeline(monkeypatch, run_summary):
    import api.index as mod

    class _Stub:
        def __init__(self, settings, progress=None):
            self._run = run_summary

        def run(self, *args, **kwargs):
            return self._run

    monkeypatch.setattr(mod, "QAPipeline", _Stub)


def test_public_index_exists():
    """The UI is served by the Vercel CDN from public/, not by Flask."""
    public = ROOT / "vercel_deploy" / "public" / "index.html"
    assert public.exists()
    assert "Jira QA Crew" in public.read_text()


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.get_json() == {"ok": True}


def test_run_without_tickets_returns_400(client):
    resp = client.post("/api/run", json={"tickets": []})
    assert resp.status_code == 400
    assert "ticket" in resp.get_json()["error"].lower()


def test_run_with_invalid_ticket_returns_400(client):
    resp = client.post("/api/run", json={"tickets": ["not-a-ticket"]})
    assert resp.status_code == 400


def test_run_shapes_full_response(client, monkeypatch, run_summary):
    _stub_pipeline(monkeypatch, run_summary)
    resp = client.post("/api/run", json={"tickets": ["VWO-48"]})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["run_id"] == run_summary.run_id
    assert data["successful"] is True
    assert len(data["results"]) == 1
    result = data["results"][0]
    assert result["ticket_key"] == "VWO-48"
    assert result["status"] == "COMPLETED_WITH_WARNINGS"
    assert result["analysis"]["summary"].startswith("Shopping cart")
    assert result["test_cases"]["test_cases"][0]["id"] == "VWO-48-TC-001"
    assert result["playwright"]["readiness"] == "NEEDS_CONFIGURATION"
    assert result["coverage"]["total_requirements"] == 2
    assert result["coverage"]["covered_requirements"] >= 0
    assert "requirements_analysis.md" in result["artifacts_md"]
    assert "test_cases.csv" in result["artifacts_md"]
    assert data["zip_b64"]


def test_run_errors_when_not_configured(client, monkeypatch, tmp_path):
    # Remove LLM key -> blocking_problems -> 400 before any run
    monkeypatch.delenv("LLM_API_KEY")
    monkeypatch.delenv("JIRA_EMAIL")
    monkeypatch.delenv("JIRA_API_TOKEN")
    resp = client.post("/api/run", json={"tickets": ["VWO-48"]})
    assert resp.status_code == 400
    assert "LLM" in resp.get_json()["error"] or "configured" in resp.get_json()["error"]


@pytest.mark.integration
def test_run_live_demo_mode(client, monkeypatch, fixtures_dir):
    """Real pipeline over a local fixture (costs LLM tokens). Opt-in."""
    monkeypatch.setenv("DEMO_MODE", "true")
    resp = client.post("/api/run", json={"tickets": ["VWO-48"]})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["results"][0]["source"] == "DEMO_FIXTURE"
