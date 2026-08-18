import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const dynamic = "force-dynamic";

const EXPECTED_KEYS = [
  "OPENROUTER_API_KEY",
  "OPENROUTER_BASE_URL",
  "OPENROUTER_MODEL",
  "OPENROUTER_TEMPERATURE",
  "OPENROUTER_MAX_TOKENS",
  "OPENROUTER_TIMEOUT_MS",
  "OPENROUTER_MAX_RETRIES",
  "DEEPEVAL_JUDGE_MODEL",
  "DEEPEVAL_METRIC_THRESHOLD",
  "DEEPEVAL_GATE_ON_FAILURE",
  "EVAL_SERVICE_URL",
  "DATABASE_URL",
  "TARGET_APP_URL",
  "MAX_SELF_HEAL_RETRIES",
  "MAX_AGENT_ITERATIONS",
  "TEST_EXECUTION_TIMEOUT_MS",
  "PLAYWRIGHT_HEADLESS",
];

export default async function SettingsPage() {
  const statuses = EXPECTED_KEYS.map((key) => ({
    key,
    configured: Boolean(process.env[key]),
  }));

  const configured = statuses.filter((s) => s.configured).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-serif font-semibold">Settings</h1>
        <p className="text-text-muted mt-1">
          Environment configuration status. Secret values are never displayed.
        </p>
      </div>

      <Card>
        <h2 className="text-lg font-semibold mb-2">Environment</h2>
        <p className="text-sm text-text-muted mb-4">
          {configured}/{statuses.length} variables configured.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          {statuses.map((s) => (
            <div
              key={s.key}
              className="flex items-center justify-between p-3 rounded-lg border border-border"
            >
              <span className="text-sm font-mono">{s.key}</span>
              <Badge tone={s.configured ? "success" : "danger"}>
                {s.configured ? "configured" : "missing"}
              </Badge>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <h2 className="text-lg font-semibold mb-2">How to configure</h2>
        <ol className="list-decimal list-inside text-sm text-text-muted space-y-1">
          <li>Copy <code className="font-mono">.env.example</code> to <code className="font-mono">.env</code>.</li>
          <li>Fill in <code className="font-mono">OPENROUTER_API_KEY</code> and <code className="font-mono">DATABASE_URL</code> (Neon Postgres).</li>
          <li>Set <code className="font-mono">TARGET_APP_URL</code> for execution.</li>
          <li>Run <code className="font-mono">npm run db:push</code> to apply schema.</li>
          <li>Run the eval service (see README) and point <code className="font-mono">EVAL_SERVICE_URL</code> at it.</li>
        </ol>
      </Card>
    </div>
  );
}
