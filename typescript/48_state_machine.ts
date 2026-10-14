type State = "idle" | "loading" | "success" | "error";

type Event = "FETCH" | "RESOLVE" | "REJECT" | "RESET";

type Transitions = {
  [S in State]: Partial<Record<Event, State>>;
};

const transitions: Transitions = {
  idle: { FETCH: "loading" },
  loading: { RESOLVE: "success", REJECT: "error" },
  success: { RESET: "idle", FETCH: "loading" },
  error: { RESET: "idle", FETCH: "loading" },
};

interface Machine {
  state: State;
  history: State[];
}

function send(machine: Machine, event: Event): Machine {
  const next = transitions[machine.state][event];
  if (!next) {
    console.log(`ignored ${event} in state ${machine.state}`);
    return machine;
  }
  console.log(`${machine.state} --${event}--> ${next}`);
  return { state: next, history: [...machine.history, next] };
}

let machine: Machine = { state: "idle", history: ["idle"] };

for (const event of ["FETCH", "RESOLVE", "FETCH", "REJECT", "RESET"] as Event[]) {
  machine = send(machine, event);
}

console.log("final state:", machine.state);
console.log("history:", machine.history.join(" -> "));

const isTerminal = (state: State): boolean => state === "success" || state === "error";
console.log("terminal?", isTerminal(machine.state));
