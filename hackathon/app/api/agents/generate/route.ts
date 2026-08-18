import { NextResponse } from "next/server";
import { eq } from "drizzle-orm";
import { env } from "@/lib/env";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";
import { callOpenRouter } from "@/lib/openrouter/client";
import { DEFAULT_AGENT_PROMPTS } from "@/lib/agents/prompts";

export const runtime = "nodejs";

/**
 * One-click agent generation: for each of the 6 agent roles, asks OpenRouter
 * to draft a tailored system prompt given an optional project description,
 * then upserts into agent_definitions.
 */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const projectDescription = String(body.projectDescription || "").trim();

  const results: { name: string; system_prompt: string }[] = [];

  for (const def of DEFAULT_AGENT_PROMPTS) {
    let prompt = def.system_prompt;
    if (projectDescription) {
      const metaPrompt = [
        `You are a QA automation architect.`,
        `Project description: ${projectDescription}`,
        ``,
        `Rewrite the following system prompt for the "${def.name}" agent so it is`,
        `tailored to this project. Keep the same JSON output contract.`,
        ``,
        `Current prompt:`,
        def.system_prompt,
      ].join("\n");

      try {
        const generated = await callOpenRouter([
          { role: "user", content: metaPrompt },
        ]);
        prompt = generated.trim();
      } catch (e) {
        console.warn(`Agent prompt generation failed for ${def.name}: ${e}`);
        // fall back to default
      }
    }

    const existing = await db
      .select({ id: schema.agentDefinitions.id })
      .from(schema.agentDefinitions)
      .where(eq(schema.agentDefinitions.name, def.name))
      .limit(1);

    if (existing[0]) {
      await db
        .update(schema.agentDefinitions)
        .set({ system_prompt: prompt })
        .where(eq(schema.agentDefinitions.id, existing[0].id));
    } else {
      await db.insert(schema.agentDefinitions).values({
        name: def.name,
        phase: def.phase,
        system_prompt: prompt,
        enabled: true,
      });
    }

    results.push({ name: def.name, system_prompt: prompt });
  }

  return NextResponse.json({ agents: results });
}
