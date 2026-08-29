"use client";

import { useMemo, useState } from "react";
import { AgentRow, EvalRow, OutputFile } from "@/lib/api";
import { Card, CardTitle, EvalCard, Stage, StageStepper, AgentRowView } from "@/components/shared";

// Test Automation pipeline stages: POM -> Framework -> Eval
const AUTOMATION_STAGES: Stage[] = [
  { key: 3, label: "POM", agent: "pom_writer" },
  { key: 4, label: "Framework", agent: "framework_architect" },
  { key: 5, label: "Eval", agent: "automation_eval" },
];

/**
 * Group files into a folder-tree: { folderName: [files] }.
 * Files at the root of the run folder land under "(root)".
 * Folders keep their relative path (e.g. "tests/e2e", "pages/login").
 */
function groupFilesByDir(files: OutputFile[], runId: string): Record<string, OutputFile[]> {
  const groups: Record<string, OutputFile[]> = {};
  for (const f of files) {
    const rel = f.name.startsWith(`${runId}/`) ? f.name.slice(runId.length + 1) : f.name;
    const parts = rel.split("/");
    const folder = parts.length > 1 ? parts.slice(0, -1).join("/") : "(root)";
    if (!groups[folder]) groups[folder] = [];
    groups[folder].push({ ...f, name: parts[parts.length - 1] });
  }
  return groups;
}

/**
 * Test Automation — third view. Shows the POM -> Framework -> Eval pipeline,
 * the automation agents, the DeepEval automation coverage, and the generated
 * POM + framework files (folder tree). Running tests and the release report
 * live on the Test Run & Report view.
 */
export default function AutomationView({
  runId,
  frameworkFiles,
  agents,
  evals,
}: {
  runId: string;
  frameworkFiles: OutputFile[];
  agents: AgentRow[];
  evals: EvalRow[];
}) {
  // Folders open by default so the tree is visible on first load
  const [openFolders, setOpenFolders] = useState<Set<string> | null>(null);

  const groups = useMemo(() => groupFilesByDir(frameworkFiles, runId), [frameworkFiles, runId]);

  const isOpen = (folder: string) => openFolders === null || openFolders.has(folder);

  const toggleFolder = (folder: string) =>
    setOpenFolders((prev) => {
      const base = prev === null ? new Set(Object.keys(groups)) : new Set(prev);
      if (base.has(folder)) base.delete(folder);
      else base.add(folder);
      return base;
    });

  const fileCount = frameworkFiles.length;

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-6">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Test Automation</h1>
        <p className="mt-0.5 text-sm text-ink-soft">
          {runId
            ? `Run ${runId} · output/${runId}/`
            : "Automate test cases to generate the framework. Run them on the Test Run & Report view."}
        </p>
      </div>

      {/* Stepper — always visible */}
      <Card>
        <div className="px-5 py-4">
          <StageStepper stages={AUTOMATION_STAGES} agents={agents} evals={evals} commands={[]} />
        </div>
      </Card>

      {/* Automation agents */}
      <Card>
        <CardTitle step="⌁" title="Crew at work" subtitle="Live status of the automation agents" />
        <div className="px-5 py-4">
          <ol className="flex flex-col gap-2">
            {agents.map((row) => (
              <AgentRowView key={row.key} row={row} />
            ))}
          </ol>
        </div>
      </Card>

      {/* DeepEval automation coverage */}
      {evals.length > 0 && (
        <Card>
          <CardTitle step="◎" title="Automation Evaluation" subtitle="DeepEval cross-verification" />
          <div className="px-5 py-4">
            <ol className="flex flex-col gap-2">
              {evals.map((row) => (
                <EvalCard key={row.key} row={row} />
              ))}
            </ol>
          </div>
        </Card>
      )}

      {/* Generated framework — folder tree (default content) */}
      <Card>
        <CardTitle
          step="3"
          title="Generated automation"
          subtitle={
            fileCount > 0
              ? `${fileCount} files · output/${runId}/`
              : "Nothing generated yet — automate test cases first."
          }
        />
        <div className="px-5 py-4">
          {Object.keys(groups).length > 0 ? (
            <div className="flex flex-col gap-2">
              {Object.entries(groups).map(([folder, files]) => {
                const isRoot = folder === "(root)";
                const open = isOpen(folder);
                return (
                  <div key={folder} className="rounded-lg border border-line bg-inset">
                    {/* Folder header */}
                    <button
                      onClick={() => toggleFolder(folder)}
                      className="flex w-full items-center gap-2 px-3.5 py-2.5 text-left font-mono text-xs font-semibold text-accent"
                    >
                      <span className="text-ink-soft">{open ? "▾" : "▸"}</span>
                      <span className="truncate">
                        {isRoot ? "output/" : folder + "/"}
                      </span>
                      <span className="ml-auto shrink-0 text-[10px] font-normal text-ink-soft">
                        {files.length} file{files.length === 1 ? "" : "s"}
                      </span>
                    </button>
                    {open && (
                      <div className="flex flex-col border-t border-line">
                        {files.map((f) => (
                          <details key={f.name} className="border-b border-line/60 last:border-b-0">
                            <summary className="cursor-pointer px-3.5 py-2 font-mono text-xs text-ink-soft hover:text-ink">
                              {f.name}
                            </summary>
                            <pre className="max-h-72 overflow-auto border-t border-line p-3.5 font-mono text-xs leading-relaxed whitespace-pre-wrap">
                              {f.content}
                            </pre>
                          </details>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-sm text-ink-soft">
              No framework files yet. Select test cases in Test Case Generation and click Automate
              Selected — the generated folders and files (config, pages, tests/e2e, helpers) will
              appear here.
            </p>
          )}
        </div>
      </Card>
    </div>
  );
}

