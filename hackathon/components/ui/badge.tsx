export type BadgeTone =
  | "default"
  | "success"
  | "warning"
  | "danger"
  | "accent";

const TONES: Record<BadgeTone, string> = {
  default: "bg-border/60 text-text-muted",
  success: "bg-success/15 text-success",
  warning: "bg-warning/15 text-warning",
  danger: "bg-danger/15 text-danger",
  accent: "bg-accent/15 text-accent",
};

export function Badge({
  children,
  tone = "default",
}: {
  children: React.ReactNode;
  tone?: BadgeTone;
}) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${TONES[tone]}`}
    >
      {children}
    </span>
  );
}
