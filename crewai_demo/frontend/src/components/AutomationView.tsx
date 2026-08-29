"use client";

import { useState } from "react";
import { OutputFile } from "@/lib/api";
import { Card, CardTitle } from "@/components/shared";

/**
 * Test Automation — the third view. Shows the generated POM, the framework
 * files, and runs the tests. Reached after the user automates selected test
 * cases.
 */
export default function AutomationView({
  runId,
  pomCode,
  frameworkFiles,
  commands,
  runOutput,
  runSuccess,
  busy,
  phaseLabel,
  onRunTests,
}: {
  runId: string;
  pomCode: string;
  frameworkFiles: OutputFile[];
  commands: import("@/lib/api").CommandRow[];
  runOutput: string;
  runSuccess: boolean | null;
  busy: boolean;
  phaseLabel: string;
  onRunTests: () => void;
}) {
  const [tab, setTab] = useState<"pom" | "files" | "run">("pom");

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-6">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Test Automation</h1>
        <p className="mt-0.5 text-sm text-ink-soft">
          {runId ? `Run ${runId} · output/${runId}/` : "Automate test cases to generate the framework."}
        </p>
      </div>

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

      {pomCode && (
        <Card>
          <CardTitle step="3" title="Generated automation" subtitle={`Run ${runId}`} />
          <div className="px-5 py-4">
            <div className="mb-3.5 flex gap-2">
              {(["pom", "files", "run"] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => setTab(t)}
                  className={`rounded-md border px-3 py-1.5 text-xs font-medium transition ${
                    tab === t
                      ? "border-accent bg-accent text-white"
                      : "border-line bg-inset text-ink-soft hover:border-accent/40"
                  }`}
                >
                  {t === "pom" ? "Page Object Model" : t === "files" ? "Framework Files" : "Test Run"}
                </button>
              ))}
            </div>

            {tab === "pom" && (
              <pre className="max-h-[26rem] overflow-auto rounded-lg border border-line bg-inset p-4 font-mono text-xs leading-relaxed whitespace-pre-wrap">
                {pomCode.replace(/```(typescript)?/g, "").trim()}
              </pre>
            )}

            {tab === "files" && (
              <div className="flex flex-col gap-2.5">
                {frameworkFiles.map((f) => (
                  <details key={f.name} className="rounded-lg border border-line bg-inset">
                    <summary className="cursor-pointer px-3.5 py-2.5 font-mono text-xs font-semibold text-accent">
                      {f.name.replace(`${runId}/`, "")}
                    </summary>
                    <pre className="max-h-72 overflow-auto border-t border-line p-3.5 font-mono text-xs leading-relaxed whitespace-pre-wrap">
                      {f.content}
                    </pre>
                  </details>
                ))}
              </div>
            )}

            {tab === "run" && (
              <div className="flex flex-col gap-3">
                <button
                  onClick={onRunTests}
                  disabled={busy}
                  className="self-start rounded-lg bg-ok px-5 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
                >
                  {busy && phaseLabel.includes("Phase 2b") ? "Running…" : "▶ Run Tests"}
                </button>
                {runOutput && (
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
                )}
              </div>
            )}
          </div>
        </Card>
      )}
    </div>
  );
}
