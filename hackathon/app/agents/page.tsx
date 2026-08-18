"use client";

import { useEffect, useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

interface Agent {
  id: string;
  name: string;
  phase: string;
  system_prompt: string;
  model_override?: string;
  enabled: boolean;
}

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [projectDescription, setProjectDescription] = useState("");
  const [error, setError] = useState("");

  async function load() {
    try {
      const res = await fetch("/api/agents");
      const json = await res.json();
      setAgents(json.agents || []);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function generate() {
    setGenerating(true);
    setError("");
    try {
      const res = await fetch("/api/agents/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ projectDescription }),
      });
      const json = await res.json();
      if (!res.ok) throw new Error(json.error || "Generation failed");
      await load();
    } catch (e) {
      setError(String(e));
    } finally {
      setGenerating(false);
    }
  }

  async function updateAgent(agent: Agent, patch: Partial<Agent>) {
    const res = await fetch("/api/agents", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: agent.id, ...patch }),
    });
    if (res.ok) await load();
  }

  if (loading) {
    return <p className="text-text-muted">Loading agents…</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Agents</h1>
        <p className="text-text-muted mt-1">
          Registry of the 6 STLC agents. Edit prompts or regenerate with AI.
        </p>
      </div>

      <Card>
        <h2 className="text-lg font-semibold mb-2">One-Click Agent Generation</h2>
        <p className="text-sm text-text-muted mb-3">
          Tailor the agent system prompts to your project with OpenRouter.
        </p>
        <div className="flex gap-3">
          <input
            value={projectDescription}
            onChange={(e) => setProjectDescription(e.target.value)}
            placeholder="e.g. e-commerce checkout flow"
            className="flex-1 p-3 rounded-lg border border-border bg-white/60 focus:outline-none focus:ring-2 focus:ring-accent/40"
          />
          <Button onClick={generate} disabled={generating}>
            {generating ? "Generating…" : "Generate Agents"}
          </Button>
        </div>
        {error && <p className="text-danger text-sm mt-3">{error}</p>}
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {agents.map((agent) => (
          <Card key={agent.id} className="flex flex-col">
            <div className="flex items-center justify-between mb-2">
              <div>
                <h3 className="font-semibold">{agent.name}</h3>
                <p className="text-xs text-text-muted capitalize">{agent.phase} phase</p>
              </div>
              <div className="flex items-center gap-2">
                <Badge tone={agent.enabled ? "success" : "default"}>
                  {agent.enabled ? "enabled" : "disabled"}
                </Badge>
                <Button
                  variant="outline"
                  onClick={() =>
                    updateAgent(agent, { enabled: !agent.enabled })
                  }
                >
                  Toggle
                </Button>
              </div>
            </div>
            <textarea
              defaultValue={agent.system_prompt}
              className="flex-1 min-h-40 p-3 rounded-lg border border-border bg-white/60 text-sm focus:outline-none focus:ring-2 focus:ring-accent/40"
              onBlur={(e) => {
                if (e.target.value !== agent.system_prompt) {
                  updateAgent(agent, { system_prompt: e.target.value });
                }
              }}
            />
          </Card>
        ))}
      </div>

      {agents.length === 0 && (
        <p className="text-text-muted">
          No agents in registry yet. Click “Generate Agents” to create the 6
          defaults.
        </p>
      )}
    </div>
  );
}
