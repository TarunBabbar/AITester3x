"""
CrewAI Agents — Agent 2, 3 and 4 (Brain)

Three specialised QA agents built on CrewAI, all driven by a single
OpenRouter LLM configured entirely through .env:

  1. test_case_agent   — analyses the page snapshot and writes P0-P3 test
                         cases (functional + UI + accessibility).
  2. pom_writer_agent  — turns the page snapshot + test cases into a
                         TypeScript Page Object Model (POM) file.
  3. framework_agent   — writes the complete Playwright + TypeScript test
                         framework: package.json, tsconfig, playwright
                         config and a spec file that uses the POM.

Why three agents? It mirrors a real QA pipeline: read the page -> plan the
tests -> build the automation. Each agent has a narrow role, and the output
of one feeds the next.
"""

import logging
import os
import time

from crewai import Agent, Crew, LLM, Task
from dotenv import load_dotenv

load_dotenv()

log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Step 0 - The Brain (OpenRouter OR Command Code, selected via .env)
#
# Both providers expose an OpenAI-compatible chat/completions API, so both
# work through CrewAI's LLM with the "openai/" provider prefix and a custom
# base_url. LLM_PROVIDER in .env picks which one the agents use:
#
#   LLM_PROVIDER=openrouter    -> OPENROUTER_API_KEY + OPENROUTER_MODEL
#   LLM_PROVIDER=commandcode   -> CMD_API_KEY + CMD_MODEL
#
# Shared settings (temperature, max tokens, timeout) apply to both.
# ---------------------------------------------------------------------------
def _active_provider() -> tuple[str, str, str | None, str]:
    """Return (provider, model, api_key, base_url) from .env."""
    provider = os.getenv("LLM_PROVIDER", "openrouter").lower().strip()

    if provider == "commandcode":
        return (
            "commandcode",
            os.getenv("CMD_MODEL", "deepseek/deepseek-v4-flash"),
            os.getenv("CMD_API_KEY"),
            os.getenv("CMD_BASE_URL", "https://api.commandcode.ai/provider/v1"),
        )

    return (
        "openrouter",
        os.getenv("OPENROUTER_MODEL", "nvidia/nemotron-nano-9b-v2:free"),
        os.getenv("OPENROUTER_API_KEY"),
        os.getenv("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1"),
    )


def build_llm() -> LLM:
    provider, model, api_key, base_url = _active_provider()
    log.info("Building LLM: provider=%s model=%s base_url=%s", provider, model, base_url)
    return LLM(
        model=f"openai/{model}",
        api_key=api_key,
        base_url=base_url,
        temperature=float(os.getenv("OPENROUTER_TEMPERATURE", "0.2")),
        max_tokens=int(os.getenv("OPENROUTER_MAX_TOKENS", "8000")),
        timeout=int(os.getenv("OPENROUTER_TIMEOUT_MS", "180000")),
    )


_LLM_CACHE: LLM | None = None


def get_llm() -> LLM:
    """Build the LLM once and reuse it across all agents."""
    global _LLM_CACHE
    if _LLM_CACHE is None:
        _LLM_CACHE = build_llm()
    return _LLM_CACHE


# ---------------------------------------------------------------------------
# Step 1 - Define the Agents
# ---------------------------------------------------------------------------
def _verbose() -> bool:
    return os.getenv("AGENT_VERBOSE", "false").lower() == "true"


def build_test_case_agent():
    """Agent 2: turns a page snapshot into prioritised test cases."""
    return Agent(
        role=os.getenv(
            "TEST_AGENT_ROLE",
            "Senior Test Case Designer",
        ),
        goal=os.getenv(
            "TEST_AGENT_GOAL",
            "Analyse the page snapshot and produce clear, prioritised "
            "(P0-P3) test cases covering happy path, edge cases, negative "
            "scenarios and accessibility basics.",
        ),
        backstory=os.getenv(
            "TEST_AGENT_BACKSTORY",
            "You are a senior QA engineer with 15 years of experience in "
            "test planning, risk-based testing and test case authoring.",
        ),
        llm=get_llm(),
        verbose=_verbose(),
    )


def build_pom_writer_agent():
    """Agent 3: writes a TypeScript Page Object Model from the snapshot."""
    return Agent(
        role=os.getenv(
            "POM_AGENT_ROLE",
            "Senior Test Automation Engineer (Page Objects)",
        ),
        goal=os.getenv(
            "POM_AGENT_GOAL",
            "Design a clean, typed TypeScript Page Object Model that "
            "exposes locators and actions for every interactive element "
            "found in the page snapshot.",
        ),
        backstory=os.getenv(
            "POM_AGENT_BACKSTORY",
            "You are a Playwright expert who has built Page Object Model "
            "frameworks for hundreds of web apps. You write readable, "
            "maintainable TypeScript with typed locators and actions.",
        ),
        llm=get_llm(),
        verbose=_verbose(),
    )


def build_framework_agent():
    """Agent 4: writes the complete Playwright + TypeScript framework."""
    return Agent(
        role=os.getenv(
            "FRAMEWORK_AGENT_ROLE",
            "Senior Test Framework Architect",
        ),
        goal=os.getenv(
            "FRAMEWORK_AGENT_GOAL",
            "Generate a complete, runnable Playwright + TypeScript test "
            "framework: package.json, tsconfig.json, playwright.config.ts "
            "and a spec file that uses the generated Page Object Model.",
        ),
        backstory=os.getenv(
            "FRAMEWORK_AGENT_BACKSTORY",
            "You are a principal test architect. The frameworks you write "
            "compile on the first try, follow Playwright best practices and "
            "run reliably in CI.",
        ),
        llm=get_llm(),
        verbose=_verbose(),
    )


# ---------------------------------------------------------------------------
# Step 2 - Tasks
# ---------------------------------------------------------------------------
def build_test_case_task(agent, page_snapshot: str, extra_requirements: str) -> Task:
    return Task(
        description=(
            "Analyse the page snapshot below and create 8-12 test cases.\n\n"
            f"### Page Snapshot\n{page_snapshot}\n\n"
            f"### Extra Requirements\n{extra_requirements or 'None provided.'}\n\n"
            "Classify each test case by priority (P0, P1, P2, P3).\n\n"
            "IMPORTANT: Return ONLY a valid JSON array, no markdown fences, "
            "no commentary. Each element must be exactly:\n"
            '{"id": "TC001", "title": "...", "priority": "P0", '
            '"preconditions": "...", "steps": ["...", "..."], '
            '"expected": "..."}'
        ),
        expected_output=(
            "A valid JSON array of test case objects with keys: id, title, "
            "priority, preconditions, steps (array), expected."
        ),
        agent=agent,
    )


def build_pom_task(agent, page_snapshot: str, selected_cases: str) -> Task:
    return Task(
        description=(
            "Write a TypeScript Page Object Model class for the page "
            "described in the snapshot.\n\n"
            f"### Page Snapshot\n{page_snapshot}\n\n"
            f"### Test Cases being automated\n{selected_cases}\n\n"
            "Requirements:\n"
            "- Export a class named after the page.\n"
            "- Use Playwright's Locator type for all elements.\n"
            "- Expose typed action methods for each interaction "
            "(click, fill, select, etc.).\n"
            "- Return ONLY the TypeScript code inside a single "
            "```typescript ... ``` block, no explanations."
        ),
        expected_output=(
            "One TypeScript class definition inside a code block. It must "
            "reference locators and actions for the elements seen in the "
            "snapshot."
        ),
        agent=agent,
    )


def build_framework_task(agent, page_snapshot: str, selected_cases: str, pom_code: str) -> Task:
    return Task(
        description=(
            "Generate a complete Playwright + TypeScript test framework for "
            "the page in the snapshot.\n\n"
            f"### Page Snapshot\n{page_snapshot}\n\n"
            f"### Test Cases to automate (ONLY these)\n{selected_cases}\n\n"
            f"### Page Object Model (already written, reuse it)\n{pom_code}\n\n"
            "Files to produce:\n"
            "1. package.json (dependencies: @playwright/test, typescript, "
            "@types/node)\n"
            "2. tsconfig.json\n"
            "3. playwright.config.ts (single Chromium project, baseURL set "
            "to the page URL)\n"
            "4. tests/<page>.spec.ts that imports the POM from "
            "page-objects/<pom file> and covers EACH of the selected test "
            "cases as a test() block\n\n"
            "IMPORTANT code-quality rules:\n"
            "- Never hardcode the full page URL in assertions. Use the "
            "baseURL-relative path (e.g. expect(page).toHaveURL(/inventory\\.html$/))\n"
            "- Use page.goto('/') for navigation inside the POM, not the full URL.\n"
            "- Every test.beforeEach / async fixture must use valid TS syntax:\n"
            "  async ({ page }: { page: Page }) => { ... }\n\n"
            "Return each file inside its own ```typescript or ```json code "
            "block, prefixed with a filename line like '// FILE: package.json' "
            "either before or inside the fence."
        ),
        expected_output=(
            "Four code blocks, each prefixed with a '// FILE: <path>' line "
            "so the framework writer can save them to disk verbatim."
        ),
        agent=agent,
    )


# ---------------------------------------------------------------------------
# Step 3 + 4 - Crew and kickoff
# ---------------------------------------------------------------------------
def _kickoff(crew: Crew, agent_label: str) -> str:
    """Run a crew with start/finish/duration logging."""
    provider, model, _, _ = _active_provider()
    log.info("%s | kickoff (provider=%s model=%s)...", agent_label, provider, model)
    t0 = time.perf_counter()
    try:
        result = str(crew.kickoff())
        log.info("%s | done (%d chars) | %.1fs", agent_label, len(result),
                 time.perf_counter() - t0)
        return result
    except Exception:
        log.exception("%s | FAILED after %.1fs", agent_label, time.perf_counter() - t0)
        raise


def run_test_case_agent(page_snapshot: str, extra_requirements: str) -> str:
    """Run Agent 2 (test case designer) and return its JSON array output."""
    agent = build_test_case_agent()
    task = build_test_case_task(agent, page_snapshot, extra_requirements)
    crew = Crew(
        agents=[agent],
        tasks=[task],
        verbose=os.getenv("CREW_VERBOSE", "false").lower() == "true",
    )
    return _kickoff(crew, "Agent 2 (Test Case Designer)")


def run_pom_writer_agent(page_snapshot: str, selected_cases: str) -> str:
    """Run Agent 3 (POM writer) and return the TypeScript POM code."""
    agent = build_pom_writer_agent()
    task = build_pom_task(agent, page_snapshot, selected_cases)
    crew = Crew(
        agents=[agent],
        tasks=[task],
        verbose=os.getenv("CREW_VERBOSE", "false").lower() == "true",
    )
    return _kickoff(crew, "Agent 3 (POM Writer)")


def run_framework_agent(page_snapshot: str, selected_cases: str, pom_code: str) -> str:
    """Run Agent 4 (framework architect) and return the framework files text."""
    agent = build_framework_agent()
    task = build_framework_task(agent, page_snapshot, selected_cases, pom_code)
    crew = Crew(
        agents=[agent],
        tasks=[task],
        verbose=os.getenv("CREW_VERBOSE", "false").lower() == "true",
    )
    return _kickoff(crew, "Agent 4 (Framework Architect)")


if __name__ == "__main__":
    # Standalone smoke test: generate test cases for a made-up page so you
    # can verify the LLM wiring without the UI.
    from page_reader import PageReader

    sample = PageReader().snapshot("https://example.com")
    sample_text = "\n".join(f"{k}: {v}" for k, v in sample.items())
    print(run_test_case_agent(sample_text, "Check that the main heading is visible."))
