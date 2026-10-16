interface AccountState {
  balance: number;
  transactions: number;
}

type AccountEvent =
  | { type: "deposit"; amount: number }
  | { type: "withdraw"; amount: number }
  | { type: "reset" };

function applyEvent(state: AccountState, event: AccountEvent): AccountState {
  switch (event.type) {
    case "deposit":
      return { balance: state.balance + event.amount, transactions: state.transactions + 1 };
    case "withdraw":
      return { balance: state.balance - event.amount, transactions: state.transactions + 1 };
    case "reset":
      return { balance: 0, transactions: state.transactions + 1 };
  }
}

class EventStore {
  private events: AccountEvent[] = [];

  append(event: AccountEvent): void {
    this.events.push(event);
  }

  all(): readonly AccountEvent[] {
    return this.events;
  }

  project(initial: AccountState): AccountState {
    return this.events.reduce(applyEvent, initial);
  }

  replayTo(index: number, initial: AccountState): AccountState {
    return this.events.slice(0, index).reduce(applyEvent, initial);
  }

  get length(): number {
    return this.events.length;
  }
}

const store = new EventStore();
const initial: AccountState = { balance: 0, transactions: 0 };

store.append({ type: "deposit", amount: 1000 });
store.append({ type: "withdraw", amount: 250 });
store.append({ type: "deposit", amount: 500 });
store.append({ type: "withdraw", amount: 100 });

console.log("events:", store.length);
console.log("current state:", store.project(initial));
console.log("state after 2 events:", store.replayTo(2, initial));

const balances: number[] = [];
for (let i = 0; i <= store.length; i++) {
  balances.push(store.replayTo(i, initial).balance);
}
console.log("balance timeline:", balances.join(" -> "));

store.append({ type: "reset" });
console.log("after reset:", store.project(initial));
