"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function NewPipelineForm() {
  const router = useRouter();
  const [rawRequirement, setRawRequirement] = useState("");
  const [targetAppUrl, setTargetAppUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit() {
    if (!rawRequirement.trim()) return;
    setLoading(true);
    setError("");
    try {
      const res = await fetch("/api/pipelines", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          rawRequirement,
          targetAppUrl: targetAppUrl || undefined,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Failed to start pipeline");
        setLoading(false);
        return;
      }
      router.push(`/pipelines/${data.runId}`);
    } catch (e) {
      setError(String(e));
      setLoading(false);
    }
  }

  return (
    <Card>
      <h2 className="text-lg font-semibold mb-2">New Pipeline</h2>
      <p className="text-sm text-text-muted mb-4">
        Paste a requirement and run the full AI STLC pipeline.
      </p>
      <textarea
        value={rawRequirement}
        onChange={(e) => setRawRequirement(e.target.value)}
        placeholder="e.g. As a user, I can log in with email and password. Failed attempts lock the account after 5 tries. Passwords must be at least 8 characters..."
        className="w-full min-h-40 p-3 rounded-lg border border-border bg-white/60 focus:outline-none focus:ring-2 focus:ring-accent/40"
      />
      <input
        value={targetAppUrl}
        onChange={(e) => setTargetAppUrl(e.target.value)}
        placeholder="Target app URL (optional — overrides TARGET_APP_URL)"
        className="w-full mt-3 p-3 rounded-lg border border-border bg-white/60 focus:outline-none focus:ring-2 focus:ring-accent/40"
      />
      {error && <p className="text-danger text-sm mt-3">{error}</p>}
      <div className="mt-4">
        <Button onClick={handleSubmit} disabled={loading || !rawRequirement.trim()}>
          {loading ? "Starting…" : "Run Pipeline"}
        </Button>
      </div>
    </Card>
  );
}
