"""
Test Case Generator Agent (CrewAI + OpenRouter)

The FIRST AI agent: takes a feature requirement as free-text input and
generates a prioritised (P0-P3) set of test cases for it.

Stack:
  - CrewAI for the Agent / Task / Crew orchestration
  - OpenRouter's OpenAI-compatible API for the LLM
  - Model: nvidia/nemotron-nano-9b-v2:free (free tier)

Configuration (no hardcoding here) lives in the .env file next to this
script. Copy .env.example to .env and fill in your OpenRouter key.

Run:
  python test_case_generation.py
"""

import os

from crewai import Agent, Crew, LLM, Task
from dotenv import load_dotenv

# ---------------------------------------------------------------------------
# Step 0 - Load environment variables (reads .env from this folder)
# ---------------------------------------------------------------------------
load_dotenv()

# ---------------------------------------------------------------------------
# Step 0 - Set up the Brain (OpenRouter LLM)
#
# OpenRouter exposes an OpenAI-compatible API, so we use the "openai/"
# provider prefix with a custom base_url pointing at OpenRouter.
# All values are pulled from .env so nothing is hardcoded.
# ---------------------------------------------------------------------------
llm = LLM(
    model=f"openai/{os.getenv('OPENROUTER_MODEL', 'nvidia/nemotron-nano-9b-v2:free')}",
    api_key=os.getenv("OPENROUTER_API_KEY"),
    base_url=os.getenv("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1"),
    temperature=float(os.getenv("OPENROUTER_TEMPERATURE", "0.2")),
    max_tokens=int(os.getenv("OPENROUTER_MAX_TOKENS", "8000")),
    timeout=int(os.getenv("OPENROUTER_TIMEOUT_MS", "120000")),
)

# ---------------------------------------------------------------------------
# Step 1 - Define the Agent (identity)
# ---------------------------------------------------------------------------
test_analyst = Agent(
    role=os.getenv(
        "AGENT_ROLE",
        "Senior Test Analyst",
    ),
    goal=os.getenv(
        "AGENT_GOAL",
        "Analyse the given feature requirement and generate clear, "
        "prioritised (P0-P3) test cases that cover happy path, edge cases "
        "and negative scenarios.",
    ),
    backstory=os.getenv(
        "AGENT_BACKSTORY",
        "You are a senior QA engineer with 15 years of experience in test "
        "planning, risk-based testing and test case authoring.",
    ),
    llm=llm,
    verbose=os.getenv("AGENT_VERBOSE", "true").lower() == "true",
)

# ---------------------------------------------------------------------------
# Step 2 - Give the Task to the Agent
#
# The user's requirement text is injected here. Default to the VWO login
# example if none is passed.
# ---------------------------------------------------------------------------
requirement = os.getenv(
    "REQUIREMENT",
    "app.vwo.com Login page with username, password, a submit button and "
    "a 'Remember Me' checkbox functionality.",
)

test_case_task = Task(
    description=(
        "Analyse the following feature requirement and create 5-10 test "
        "cases for it.\n\n"
        f"### Feature Requirement\n{requirement}\n\n"
        "Classify each test case by priority (P0, P1, P2, P3)."
    ),
    expected_output=(
        "A numbered list of 5-10 test cases, each with:\n"
        "- Test case ID\n"
        "- Title\n"
        "- Preconditions\n"
        "- Test steps\n"
        "- Expected result\n"
        "- Priority (P0-P3)"
    ),
    agent=test_analyst,
)

# ---------------------------------------------------------------------------
# Step 3 - Add the Agent + Task to a Crew
# ---------------------------------------------------------------------------
crew = Crew(
    agents=[test_analyst],
    tasks=[test_case_task],
    verbose=os.getenv("CREW_VERBOSE", "true").lower() == "true",
)

# ---------------------------------------------------------------------------
# Step 4 - Kick Off
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    result = crew.kickoff()
    print("\n" + "=" * 60)
    print("GENERATED TEST CASES")
    print("=" * 60)
    print(result)