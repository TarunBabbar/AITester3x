"use client";

import { useState } from "react";
import { Card } from "@/components/shared";

/**
 * Home — the landing view. Only the URL + requirements input and the
 * "Generate Test Cases" CTA. Everything else lives on other views.
 */
export default function HomeView({
  onGenerate,
}: {
  onGenerate: (url: string, requirements: string) => void;
}) {
  const [url, setUrl] = useState("https://saucedemo.com");
  const [requirements, setRequirements] = useState("");
  const [error, setError] = useState("");

  function submit() {
    if (!url.trim()) {
      setError("Enter a page URL first.");
      return;
    }
    setError("");
    onGenerate(url.trim(), requirements.trim());
  }

  return (
    <div className="mx-auto flex max-w-xl flex-col justify-center gap-5 px-6 py-10">
      <div className="text-center">
        <h1 className="text-2xl font-bold tracking-tight">Generate Test Cases</h1>
        <p className="mt-1 text-sm text-ink-soft">
          Point the crew at a page — the agents will read it, analyse it and
          draft prioritised test cases.
        </p>
      </div>

      <Card>
        <div className="flex flex-col gap-4 px-6 py-6">
          <div>
            <label className="mb-1 block text-[11px] font-semibold uppercase tracking-wider text-ink-soft">
              Page URL
            </label>
            <input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://saucedemo.com"
              className="w-full rounded-lg border border-line bg-inset px-3.5 py-2.5 text-sm outline-none transition focus:border-accent"
            />
          </div>
          <div>
            <label className="mb-1 block text-[11px] font-semibold uppercase tracking-wider text-ink-soft">
              Extra requirements <span className="font-normal normal-case">(optional)</span>
            </label>
            <textarea
              value={requirements}
              onChange={(e) => setRequirements(e.target.value)}
              rows={3}
              placeholder="e.g. Focus on negative login cases and validation messages."
              className="w-full resize-none rounded-lg border border-line bg-inset px-3.5 py-2.5 text-sm outline-none transition focus:border-accent"
            />
          </div>

          {error && (
            <div className="rounded-lg border border-err/30 bg-err-soft px-3.5 py-2.5 text-sm text-err">
              {error}
            </div>
          )}

          <button
            onClick={submit}
            className="rounded-lg bg-accent px-5 py-2 text-sm font-semibold text-white transition hover:bg-accent-deep"
          >
            Generate Test Cases
          </button>
        </div>
      </Card>

      <p className="text-center text-xs text-ink-soft">
        You'll be taken to the pipeline view while the crew works.
      </p>
    </div>
  );
}
