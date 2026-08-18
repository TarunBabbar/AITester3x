import { chromium as pwChromium } from "playwright-core";
import { env } from "@/lib/env";

export interface ScriptResult {
  passed: boolean;
  error: string;
  logs: string;
}

let chromiumPromise: Promise<{
  executablePath: () => Promise<string>;
  args: string[];
}> | null = null;

async function getChromium() {
  if (!chromiumPromise) {
    chromiumPromise = (async () => {
      const chromium = await import("@sparticuz/chromium");
      const c = chromium.default;
      return {
        executablePath: () => c.executablePath(),
        args: c.args,
      };
    })();
  }
  return chromiumPromise;
}

/**
 * Runs a generated Playwright script (CommonJS string) in a sandboxed VM.
 * Injects TARGET_URL as a global so generated scripts can reference it.
 * `require('playwright-core')` resolves to a chromium whose launch() uses the
 * @sparticuz/chromium binary (Vercel-serverless-compatible).
 */
export async function runScript(
  script: string,
  targetUrl: string,
  headless: boolean
): Promise<ScriptResult> {
  const logs: string[] = [];
  const captured = { passed: false, error: "" };

  const consoleLog = (...args: unknown[]) =>
    logs.push(args.map(String).join(" "));
  const consoleError = (...args: unknown[]) => {
    const msg = args.map(String).join(" ");
    logs.push("ERROR: " + msg);
    if (msg.includes("TEST_FAIL")) captured.error = msg;
  };

  const sandboxChromium = {
    launch: async (opts: Record<string, unknown> = {}) => {
      const c = await getChromium();
      const executablePath = await c.executablePath();
      return pwChromium.launch({
        ...opts,
        executablePath,
        args: c.args,
        headless,
      });
    },
  };

  const sandbox = {
    console: { log: consoleLog, error: consoleError, warn: consoleLog },
    process: {
      exit: (code: number) => {
        if (code === 0) captured.passed = true;
        else captured.error = captured.error || `exit(${code})`;
        throw new SandboxExit(code);
      },
      env: {},
    },
    require: (id: string) => {
      if (id === "playwright-core") return { chromium: sandboxChromium };
      throw new Error(`Module '${id}' not available in sandbox`);
    },
    TARGET_URL: targetUrl,
    setTimeout,
    clearTimeout,
  };

  class SandboxExit extends Error {
    constructor(public code: number) {
      super(`exit(${code})`);
    }
  }

  const asyncWrapper = `
    (async () => {
      ${script}
    })();
  `;

  try {
    const fn = new Function(
      "console",
      "process",
      "require",
      "TARGET_URL",
      "setTimeout",
      "clearTimeout",
      "SandboxExit",
      asyncWrapper
    );
    await Promise.race([
      fn(
        sandbox.console,
        sandbox.process,
        sandbox.require,
        sandbox.TARGET_URL,
        setTimeout,
        clearTimeout,
        SandboxExit
      ),
      new Promise((_, reject) =>
        setTimeout(
          () => reject(new Error(`Test execution exceeded ${env.TEST_EXECUTION_TIMEOUT_MS}ms`)),
          env.TEST_EXECUTION_TIMEOUT_MS
        )
      ),
    ]);
    captured.passed = true;
  } catch (e) {
    if (!(e instanceof SandboxExit) || e.code !== 0) {
      captured.error = captured.error || String(e);
    }
  }

  return {
    passed: captured.passed,
    error: captured.error,
    logs: logs.join("\n"),
  };
}
