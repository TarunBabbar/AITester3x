"use client";

import { useMemo, useState } from "react";
import { CommandRow, EvalRow, OutputFile } from "@/lib/api";
import { Card, CardTitle, EvalCard } from "@/components/shared";

/** Extract playwright test() titles from a spec file. */
function extractTestNames(files: OutputFile[]): string[] {
  const names: string[] = [];
  for (const f of files) {
    if (!f.name.endsWith(".spec.ts")) continue;
    const re = /(?:test|test\.skip)\(['"](.+?)['"]/g;
    let m: RegExpExecArray | null;
    while ((m = re.exec(f.content)) !== null) names.push(m[1]);
  }
  return names;
}

/**
 * Test Run & Report — fourth view. Run the generated tests (after Docker is
 * up), watch the commands stream, then review the Release Confidence Report:
 * each requirement mapped through the pipeline, which tests passed/failed,
 * and the overall weighted confidence.
 */
export default function RunView({
  runId,
  frameworkFiles,
  evals,
  commands,
  runOutput,
  runSuccess,
  releaseScore,
  docker,
  busy,
  phaseLabel,
  onRunTests,
  onStop,
}: {
  runId: string;
  frameworkFiles: OutputFile[];
  evals: EvalRow[];
  commands: CommandRow[];
  runOutput: string;
  runSuccess: boolean | null;
  releaseScore: number | null;
  docker: { available: boolean } | null;
  busy: boolean;
  phaseLabel: string;
  onRunTests: (testNames: string[]) => void;
  onStop: () => void;
}) {
  const testNames = useMemo(() => extractTestNames(frameworkFiles), [frameworkFiles]);
  const [selected, setSelected] = useState<Set<string>>(new Set(testNames));

  const toggleTest = (name: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });

  // docker: undefined = unknown (still checking), false = down, true = up
  const dockerReady = docker?.available ?? false;
  const dockerUnknown = docker === null;
  const anyCommandRunning = commands.some((c) => c.state === "running");

  // Parse per-test pass/fail from the playwright list output
  const testResults = useMemo(() => {
    if (!runOutput) return [];
    const results: { name: string; passed: boolean }[] = [];
    const re = /(✓|✘|×)\s+\d+\s+\[[^\]]*\]\s+.+?\s+›\s+(.+?)\s+\(/g;
    let m: RegExpExecArray | null;
    while ((m = re.exec(runOutput)) !== null) {
      results.push({ name: m[2].trim(), passed: m[1] === "✓" });
    }
    return results;
  }, [runOutput]);

  const passedCount = testResults.filter((t) => t.passed).length;
  const totalCount = testResults.length;

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold tracking-tight">Test Run & Report</h1>
          <p className="mt-0.5 text-sm text-ink-soft">
            {runId
              ? `Run ${runId} · output/${runId}/`
              : "Run the generated tests and review the release confidence report."}
          </p>
        </div>
        {anyCommandRunning && (
          <button
            onClick={onStop}
            className="shrink-0 rounded-lg border border-err/40 bg-err-soft px-4 py-2 text-sm font-semibold text-err transition hover:bg-err/20"
          >
            ■ Stop
          </button>
        )}
      </div>

      {/* Docker requirement */}
      <Card>
        <CardTitle
          step="🐳"
          title="Docker required to run tests"
          subtitle="The test runner needs the Docker engine"
        />
        <div className="px-5 py-4">
          <div
            className={`flex items-center gap-3 rounded-lg border px-4 py-3 text-sm ${
              dockerReady
                ? "border-ok/30 bg-ok-soft text-ok"
                : dockerUnknown
                  ? "border-warn/40 bg-warn-soft text-warn"
                  : "border-err/40 bg-err-soft text-err"
            }`}
          >
            <span className="size-2.5 shrink-0 rounded-full bg-current" />
            {dockerReady
              ? "Docker engine is running — ready to execute tests."
              : dockerUnknown
                ? "Checking Docker engine…"
                : "Docker is NOT running. Start Docker Desktop and wait for the engine — tests cannot run until it is up."}
          </div>
        </div>
      </Card>

      {/* Select tests + run */}
      <Card>
        <CardTitle
          step="▶"
          title="Run tests"
          subtitle={testNames.length > 0 ? `${testNames.length} generated tests` : "No tests generated yet"}
        />
        <div className="px-5 py-4">
          {testNames.length > 0 ? (
            <div className="flex flex-col gap-3">
              <div className="flex flex-col gap-2">
                {testNames.map((name) => (
                  <label
                    key={name}
                    className={`flex cursor-pointer items-center gap-3 rounded-lg border px-3.5 py-2.5 transition ${
                      selected.has(name)
                        ? "border-accent/40 bg-accent-soft/40"
                        : "border-line bg-inset hover:border-line-soft"
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={selected.has(name)}
                      onChange={() => toggleTest(name)}
                      className="size-4 accent-accent"
                    />
                    <span className="min-w-0 truncate font-mono text-xs">{name}</span>
                  </label>
                ))}
              </div>

              <div className="flex gap-2.5">
                <button
                  onClick={() =>
                    setSelected((prev) =>
                      prev.size === testNames.length ? new Set() : new Set(testNames),
                    )
                  }
                  disabled={busy}
                  className="rounded-lg border border-line px-4 py-2 text-sm font-medium transition hover:border-accent/50 disabled:opacity-50"
                >
                  {selected.size === testNames.length ? "Deselect All" : "Select All"}
                </button>
                <button
                  onClick={() => onRunTests(Array.from(selected))}
                  disabled={busy || selected.size === 0 || !dockerReady}
                  className="flex-1 rounded-lg bg-ok px-5 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
                >
                  {!dockerReady
                    ? "Start Docker first"
                    : busy && phaseLabel.includes("Phase 2b")
                      ? "Running…"
                      : `▶ Run Selected (${selected.size})`}
                </button>
              </div>
            </div>
          ) : (
            <p className="text-sm text-ink-soft">
              No generated tests yet. Go to Test Automation and automate selected test cases first —
              they will appear here to pick and run.
            </p>
          )}
        </div>
      </Card>

      {/* Live commands while running */}
      {commands.length > 0 && (
        <Card>
          <CardTitle step="⌁" title="Running tests" subtitle={phaseLabel} />
          <div className="px-5 py-4">
            <ol className="flex flex-col gap-2">
              {commands.map((row, i) => (
                <li
                  key={`${row.command}-${i}`}
                  className="flex items-center justify-between gap-3 rounded-lg border border-line bg-inset px-3.5 py-2.5 font-mono text-xs"
                >
                  <span className="min-w-0 truncate text-ink-soft">
                    <span className="text-accent">$</span> {row.command}
                  </span>
                  <span
                    className={`shrink-0 ${
                      row.state === "done"
                        ? row.exitCode === 0
                          ? "text-ok"
                          : "text-err"
                        : "text-warn"
                    }`}
                  >
                    {row.state === "done"
                      ? `exit ${row.exitCode} · ${((row.durationMs ?? 0) / 1000).toFixed(1)}s`
                      : "running…"}
                  </span>
                </li>
              ))}
            </ol>
          </div>
        </Card>
      )}

      {/* Test output */}
      {runOutput && (
        <Card>
          <CardTitle step="⌁" title="Test output" subtitle="Playwright result" />
          <div className="px-5 py-4">
            <pre
              className={`max-h-[26rem] overflow-auto rounded-lg border p-4 font-mono text-xs leading-relaxed whitespace-pre-wrap ${
                runSuccess === true
                  ? "border-ok/30 bg-ok-soft"
                  : runSuccess === false
                    ? "border-err/30 bg-err-soft"
                    : "border-line bg-inset"
              }`}
            >
              {runOutput}
            </pre>
          </div>
        </Card>
      )}

      {/* Release Confidence Report */}
      <Card>
        <CardTitle
          step="★"
          title="Release Confidence Report"
          subtitle="Requirements → test cases → automation → results"
        />
        <div className="flex flex-col gap-4 px-5 py-5">
          {/* Overall score */}
          <div className="flex items-center gap-4 rounded-lg border border-line bg-inset px-4 py-4">
            <span
              className={`font-display text-5xl font-bold ${
                releaseScore !== null && releaseScore >= 0.7
                  ? "text-ok"
                  : releaseScore !== null && releaseScore >= 0.4
                    ? "text-warn"
                    : "text-err"
              }`}
            >
              {releaseScore !== null ? `${(releaseScore * 100).toFixed(0)}%` : "—"}
            </span>
            <div className="text-sm text-ink-soft">
              <p>
                {releaseScore === null
                  ? "Run the tests to compute the release confidence from all evaluations."
                  : releaseScore >= 0.7
                    ? "Looking good — requirements are well covered and tests pass. Ready to ship."
                    : releaseScore >= 0.4
                      ? "Moderate coverage — review the evaluation reasons and failing tests."
                      : "Low coverage — too many gaps to release confidently."}
              </p>
              {testResults.length > 0 && (
                <p className="mt-1 font-mono text-xs">
                  {passedCount}/{totalCount} tests passed
                </p>
              )}
            </div>
          </div>

          {/* DeepEval metric breakdown */}
          {evals.length > 0 && (
            <div>
              <p className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-ink-soft">
                DeepEval stage scores
              </p>
              <ol className="flex flex-col gap-2">
                {evals.map((row) => (
                  <EvalCard key={row.key} row={row} />
                ))}
              </ol>
            </div>
          )}

          {/* Per-test results */}
          {testResults.length > 0 && (
            <div>
              <p className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-ink-soft">
                Test results
              </p>
              <ol className="flex flex-col gap-2">
                {testResults.map((t, i) => (
                  <li
                    key={`${t.name}-${i}`}
                    className="flex items-center gap-3 rounded-lg border border-line bg-inset px-3.5 py-2.5 text-sm"
                  >
                    <span className={t.passed ? "text-ok" : "text-err"}>
                      {t.passed ? "✓" : "✘"}
                    </span>
                    <span className="min-w-0 flex-1 truncate font-mono text-xs">{t.name}</span>
                    <span
                      className={`shrink-0 rounded border px-1.5 py-0.5 font-mono text-[10px] font-bold ${
                        t.passed
                          ? "border-ok/40 bg-ok-soft text-ok"
                          : "border-err/40 bg-err-soft text-err"
                      }`}
                    >
                      {t.passed ? "PASSED" : "FAILED"}
                    </span>
                  </li>
                ))}
              </ol>
            </div>
          )}

          {releaseScore === null && testResults.length === 0 && (
            <p className="text-sm text-ink-soft">
              No report yet. Run the tests and the confidence report will appear here, mapping every
              requirement through evaluation and test results.
            </p>
          )}
        </div>
      </Card>
    </div>
  );
}
