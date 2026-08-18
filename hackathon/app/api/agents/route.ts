import { NextResponse } from "next/server";
import { eq } from "drizzle-orm";
import { db } from "@/lib/db/client";
import * as schema from "@/lib/db/schema";

export const runtime = "nodejs";

export async function GET() {
  const agents = await db.select().from(schema.agentDefinitions);
  return NextResponse.json({ agents });
}

export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const name = String(body.name || "");
  const phase = String(body.phase || "");
  const systemPrompt = String(body.systemPrompt || "");

  if (!name || !phase || !systemPrompt) {
    return NextResponse.json(
      { error: "name, phase, systemPrompt are required" },
      { status: 400 }
    );
  }

  const [agent] = await db
    .insert(schema.agentDefinitions)
    .values({
      name,
      phase,
      system_prompt: systemPrompt,
      model_override: body.modelOverride ? String(body.modelOverride) : null,
      enabled: body.enabled !== false,
    })
    .returning();

  return NextResponse.json({ agent }, { status: 201 });
}

export async function PATCH(req: Request) {
  const body = await req.json().catch(() => ({}));
  const id = String(body.id || "");
  if (!id) {
    return NextResponse.json({ error: "id is required" }, { status: 400 });
  }

  const updates: Partial<typeof schema.agentDefinitions.$inferInsert> = {};
  if (body.systemPrompt !== undefined) updates.system_prompt = String(body.systemPrompt);
  if (body.enabled !== undefined) updates.enabled = Boolean(body.enabled);
  if (body.modelOverride !== undefined)
    updates.model_override = body.modelOverride ? String(body.modelOverride) : null;

  const [agent] = await db
    .update(schema.agentDefinitions)
    .set(updates)
    .where(eq(schema.agentDefinitions.id, id))
    .returning();

  return NextResponse.json({ agent });
}
