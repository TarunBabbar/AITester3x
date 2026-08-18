import { z } from "zod";
import { env } from "@/lib/env";
import { callOpenRouter, ChatMessage } from "@/lib/openrouter/client";

/**
 * Parses JSON from an LLM response. Tolerates markdown code fences, leading
 * prose, and trailing prose after the JSON value.
 */
export function parseLlmJson<T extends z.ZodTypeAny>(
  raw: string,
  schema: T
): z.infer<T> {
  let text = raw.trim();

  const fenceMatch = text.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (fenceMatch) text = fenceMatch[1].trim();

  // Direct parse first.
  try {
    return schema.parse(JSON.parse(text));
  } catch {
    // fall through to tolerant parse
  }

  // Find the first { or [ ... strip leading prose.
  const start = text.search(/[\[{]/);
  if (start > 0) text = text.slice(start);

  // Try progressively shorter suffixes — the model often appends trailing prose.
  for (let i = text.length; i > 0; i--) {
    const candidate = text.slice(0, i);
    const trimmed = candidate.trim();
    if (!trimmed) continue;
    if (!/[}\]]$/.test(trimmed)) continue;
    try {
      return schema.parse(JSON.parse(trimmed));
    } catch {
      // keep shrinking
    }
  }

  throw new Error(`Failed to parse LLM JSON. Raw: ${raw.slice(0, 800)}`);
}

/** Builds a system prompt fragment asking for JSON-only output. */
export function jsonOnlyInstruction(): string {
  return [
    "You MUST respond with valid JSON only.",
    "Do NOT wrap it in markdown code fences.",
    "Do NOT include any prose outside the JSON object.",
  ].join("\n");
}

/**
 * Calls the model, parses the JSON response against the schema. If parsing
 * fails (free-tier models truncate or add prose), sends a repair retry with
 * the failed output so the model can complete/fix it. Keeps total calls bounded.
 */
export async function callWithJsonRepair<T extends z.ZodTypeAny>(
  messages: ChatMessage[],
  schema: T,
  opts?: { model?: string; temperature?: number; maxTokens?: number }
): Promise<{ rawText: string; output: z.infer<T> }> {
  let rawText = await callOpenRouter(messages, opts);

  try {
    return { rawText, output: parseLlmJson(rawText, schema) };
  } catch (e) {
    console.warn("JSON parse failed, attempting repair:", String(e).slice(0, 200));
    const repairPrompt = [
      "Your previous response was not valid JSON. Here it is:",
      "```",
      rawText.slice(0, 6000),
      "```",
      "Fix it: return the COMPLETE corrected JSON only, no prose, no markdown fences.",
    ].join("\n");

    const fixed = await callOpenRouter(
      [
        { role: "system", content: "You fix truncated or malformed JSON. Return only valid JSON." },
        { role: "user", content: repairPrompt },
      ],
      opts
    );

    const repaired = parseLlmJson(fixed, schema);
    return { rawText: fixed, output: repaired };
  }
}
