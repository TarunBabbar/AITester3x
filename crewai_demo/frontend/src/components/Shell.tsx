"use client";

export type ViewKey = "home" | "pipeline" | "automation";

const NAV: { key: ViewKey; label: string; icon: string }[] = [
  { key: "home", label: "Home", icon: "⌂" },
  { key: "pipeline", label: "Test Case Generation", icon: "⌁" },
  { key: "automation", label: "Test Automation", icon: "⚙" },
];

/**
 * App shell: left navigation + main content area. The nav persists across
 * views; each view owns its own state and the router keeps it mounted while
 * hidden so the pipeline keeps streaming when you switch views.
 */
export default function Shell({
  view,
  onNavigate,
  provider,
  model,
  health,
  children,
}: {
  view: ViewKey;
  onNavigate: (v: ViewKey) => void;
  provider: string;
  model: string;
  health: { status: string } | null;
  children: React.ReactNode;
}) {
  return (
    <div className="page-glow flex min-h-screen">
      {/* Left navigation */}
      <aside className="sticky top-0 flex h-screen w-56 shrink-0 flex-col border-r border-line bg-panel/60 px-3 py-4 backdrop-blur">
        <div className="mb-6 flex items-center gap-2.5 px-2">
          <span className="grid size-8 place-items-center rounded-lg bg-accent-soft text-sm text-accent">⚙</span>
          <div>
            <p className="text-sm font-bold leading-tight">CrewAI QA Studio</p>
            <p className="text-[10px] text-ink-soft">Playwright automation crew</p>
          </div>
        </div>

        <nav className="flex flex-col gap-1">
          {NAV.map((item) => (
            <button
              key={item.key}
              onClick={() => onNavigate(item.key)}
              className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm transition ${
                view === item.key
                  ? "bg-accent-soft font-semibold text-accent"
                  : "text-ink-soft hover:bg-inset hover:text-ink"
              }`}
            >
              <span className="w-4 text-center">{item.icon}</span>
              {item.label}
            </button>
          ))}
        </nav>

        <div className="mt-auto flex flex-col gap-2 border-t border-line px-2 pt-3">
          <div className="flex items-center gap-1.5 text-[10px]">
            <span
              className={`size-1.5 rounded-full ${
                health?.status === "ok" ? "bg-ok" : "bg-err"
              }`}
            />
            <span className="text-ink-soft">
              {health ? "backend online" : "backend offline"}
            </span>
          </div>
          {model && (
            <p className="truncate font-mono text-[10px] text-ink-soft" title={`${provider} · ${model}`}>
              {provider} · {model}
            </p>
          )}
        </div>
      </aside>

      {/* Main content */}
      <main className="min-w-0 flex-1">{children}</main>
    </div>
  );
}
