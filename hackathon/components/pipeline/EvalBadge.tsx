import { Badge, BadgeTone } from "../ui/badge";

export function EvalBadge({
  score,
  threshold,
}: {
  score?: number;
  threshold?: number;
}) {
  if (score === undefined) {
    return <Badge tone="default">no eval</Badge>;
  }

  const tone: BadgeTone =
    threshold !== undefined && score < threshold ? "danger" : "success";

  return (
    <Badge tone={tone}>
      eval {Math.round(score * 100)}%
      {threshold !== undefined && (
        <span className="opacity-70"> / {Math.round(threshold * 100)}%</span>
      )}
    </Badge>
  );
}
