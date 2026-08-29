"""
Test Runner

Runs the Playwright + TypeScript framework that the Framework Agent
generated. Steps:

  1. Ensure node/npm exist (Playwright runs on Node, not Python).
  2. `npm install` in the output dir (auto-install is configurable).
  3. Optionally `npx playwright install chromium` (configurable; the
     browser download is large so it defaults to off).
  4. `npx playwright test --reporter=list` and stream the output.

Everything is configured through .env (auto-install, browser install).
The command runs in a subprocess so long test runs never block the API.
"""

import logging
import os
import subprocess
import time
from pathlib import Path

from dotenv import load_dotenv

from framework_writer import OUTPUT_DIR

load_dotenv()

log = logging.getLogger(__name__)


def _bool_env(name: str, default: str) -> bool:
    return os.getenv(name, default).lower() == "true"


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------
def node_available() -> tuple[bool, str]:
    """Return (available, version_or_error_message)."""
    try:
        result = subprocess.run(
            ["node", "--version"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        if result.returncode == 0:
            return True, result.stdout.strip()
        return False, result.stderr.strip() or "node returned a non-zero exit code"
    except FileNotFoundError:
        return False, "node not found on PATH"
    except subprocess.TimeoutExpired:
        return False, "node --version timed out"


def npm_available() -> bool:
    try:
        result = subprocess.run(
            ["npm", "--version"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        return result.returncode == 0
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------
class TestRunner:
    """Runs the generated Playwright framework and captures its output."""

    def __init__(self, output_dir: Path | None = None) -> None:
        self.output_dir = output_dir or OUTPUT_DIR
        self.auto_install = _bool_env("TEST_RUNNER_AUTO_INSTALL", "true")
        self.browser_install = _bool_env("TEST_RUNNER_BROWSER_INSTALL", "false")
        self.timeout = int(os.getenv("TEST_RUNNER_TIMEOUT_MS", "600000"))

    def run(self) -> dict:
        """Execute the framework and return {success, output, log_file}."""
        logs_dir = self.output_dir / "logs"
        logs_dir.mkdir(parents=True, exist_ok=True)
        log_file = logs_dir / "test_run.log"

        with open(log_file, "w", encoding="utf-8") as fh:
            lines = []
            for line in self._commands():
                fh.write(f"$ {line}\n")
                fh.flush()
                log.info("running: %s (cwd=output/%s)", line, self.output_dir.name)
                t0 = time.perf_counter()
                result = subprocess.run(
                    line,
                    shell=True,
                    cwd=self.output_dir,
                    capture_output=True,
                    text=True,
                    timeout=self.timeout,
                )
                combined = (result.stdout or "") + (result.stderr or "")
                fh.write(combined + "\n")
                fh.flush()
                lines.append(combined)
                log.info("finished: %s | exit=%d | %.1fs",
                         line, result.returncode, time.perf_counter() - t0)
                if result.returncode != 0:
                    log.error("command failed (exit=%d): %s", result.returncode, line)
                    break

        full_output = "\n".join(lines)
        return {
            "success": "passed" in full_output.lower() and "failed" not in full_output.lower(),
            "output": full_output[-6000:],
            "log_file": str(log_file),
        }

    def _commands(self) -> list[str]:
        """Build the command sequence for this environment."""
        commands: list[str] = []
        if self.auto_install:
            # --include=dev overrides a global "omit=dev" npm config so
            # @playwright/test & typescript actually get installed.
            commands.append("npm install --include=dev")
        if self.browser_install:
            commands.append("npx playwright install chromium")
        commands.append("npx playwright test --reporter=list")
        return commands


if __name__ == "__main__":
    ok, msg = node_available()
    print(f"node: {msg if ok else 'MISSING — ' + msg}")
    print(f"npm:  {'available' if npm_available() else 'MISSING'}")
    if ok and (Path(OUTPUT_DIR) / "package.json").exists():
        print("Running generated framework...")
        result = TestRunner().run()
        print(result["success"] and "TESTS PASSED" or "TESTS FAILED")
        print(result["output"])
