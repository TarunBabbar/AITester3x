import { Badge, BadgeTone } from "../ui/badge";

const PHASE_STEPS = [
  "requirement",
  "planning",
  "testcase",
  "execution",
  "triage",
  "reporting",
];

function statusTone(status?: string): BadgeTone {
  switch (status) {
    case "passed":
    case "completed":
      return "success";
    case "running":
    case "pending":
      return "warning";
    case "failed":
    case "blocked":
      return "danger";
    default:
      return "default";
  }
}

export interface PhaseStepState {
  phase: string;
  status?: string;
  score?: number;
}

export function PhaseTimeline({
  currentPhase,
  runStatus,
  scores,
}: {
  currentPhase?: string;
  runStatus?: string;
  scores?: { agent_name: string; score: number }[];
}) {
  const currentIdx = currentPhase
    ? PHASE_STEPS.indexOf(currentPhase)
    : -1;

  return (
    <div className="flex items-center gap-1 flex-wrap">
      {PHASE_STEPS.map((phase, idx) => {
        let status: string | undefined;
        if (runStatus === "blocked" || runStatus === "failed") {
          status = idx <= currentIdx ? "failed" : "pending";
        } else if (currentIdx === -1) {
          status = "pending";
        } else if (idx < currentIdx) {
          status = "passed";
        } else if (idx === currentIdx) {
          status = "running";
        } else {
          status = "pending";
        }
        const score = scores?.find(
          (s) => s.agent_name === `${phase}-agent`
        )?.score;

        return (
          <div key={phase} className="flex items-center gap-1">
            <div className="flex flex-col items-center px-2 py-1 rounded-lg bg-surface border border-border">
              <span className="text-xs font-medium text-text-primary capitalize">
                {phase}
              </span>
              <Badge tone={statusTone(status)}>{status || "pending"}</Badge>
              {score !== undefined && (
                <span className="text-xs text-text-muted mt-0.5">
                  {Math.round(score * 100)}%
                </span>
              )}
            </div>
            {idx < PHASE_STEPS.length - 1 && (
              <span className="text-text-muted">→</span>
            )}
          </div>
        );
      })}
    </div>
  );
}
