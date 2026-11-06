type Handler<Payload> = (payload: Payload) => void;

interface Listener {
  handler: (payload: any) => void;
  once: boolean;
  priority: number;
}

class TypedEventBus<Events extends object> {
  private listeners = new Map<keyof Events, Listener[]>();

  on<Event extends keyof Events>(event: Event, handler: Handler<Events[Event]>, priority = 0): () => void {
    return this.add(event, handler, false, priority);
  }

  once<Event extends keyof Events>(event: Event, handler: Handler<Events[Event]>, priority = 0): () => void {
    return this.add(event, handler, true, priority);
  }

  off<Event extends keyof Events>(event: Event, handler: Handler<Events[Event]>): boolean {
    const entries = this.listeners.get(event);
    if (!entries) {
      return false;
    }
    const index = entries.findIndex((entry) => entry.handler === handler);
    if (index === -1) {
      return false;
    }
    entries.splice(index, 1);
    if (entries.length === 0) {
      this.listeners.delete(event);
    }
    return true;
  }

  emit<Event extends keyof Events>(event: Event, payload: Events[Event]): number {
    const entries = this.listeners.get(event);
    if (!entries) {
      return 0;
    }
    const snapshot = [...entries].sort((left, right) => right.priority - left.priority);
    for (const entry of snapshot) {
      if (entry.once) {
        this.removeEntry(event, entry);
      }
      entry.handler(payload);
    }
    return snapshot.length;
  }

  async emitAsync<Event extends keyof Events>(event: Event, payload: Events[Event]): Promise<number> {
    const entries = this.listeners.get(event);
    if (!entries) {
      return 0;
    }
    const snapshot = [...entries].sort((left, right) => right.priority - left.priority);
    for (const entry of snapshot) {
      if (entry.once) {
        this.removeEntry(event, entry);
      }
      await entry.handler(payload);
    }
    return snapshot.length;
  }

  listenerCount<Event extends keyof Events>(event: Event): number {
    return this.listeners.get(event)?.length ?? 0;
  }

  eventNames(): (keyof Events)[] {
    return [...this.listeners.keys()];
  }

  clear(): void {
    this.listeners.clear();
  }

  private add<Event extends keyof Events>(
    event: Event,
    handler: Handler<Events[Event]>,
    once: boolean,
    priority: number,
  ): () => void {
    const entries = this.listeners.get(event) ?? [];
    entries.push({ handler: handler as (payload: any) => void, once, priority });
    this.listeners.set(event, entries);
    return () => {
      this.off(event, handler);
    };
  }

  private removeEntry(event: keyof Events, entry: Listener): void {
    const entries = this.listeners.get(event);
    if (!entries) {
      return;
    }
    const index = entries.indexOf(entry);
    if (index !== -1) {
      entries.splice(index, 1);
    }
    if (entries.length === 0) {
      this.listeners.delete(event);
    }
  }
}

type AppEvents = {
  "user:created": { id: number; name: string };
  "user:deleted": { id: number };
  "order:placed": { orderId: string; total: number };
};

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  const bus = new TypedEventBus<AppEvents>();
  const log: string[] = [];

  console.log("--- subscribe ---");
  const unsubscribe = bus.on("user:created", (payload) => log.push(`high ${payload.name}`), 10);
  bus.on("user:created", (payload) => log.push(`low ${payload.id}`), 0);
  bus.once("user:created", (payload) => log.push(`once ${payload.name}`));

  console.log("listeners:", bus.listenerCount("user:created"));

  const delivered = bus.emit("user:created", { id: 1, name: "Ada" });
  console.log("delivered:", delivered);
  console.log("priority order:", log.join(" | "));
  check(delivered === 3, "three listeners should run");
  check(log.join(" | ") === "high Ada | low 1 | once Ada", "priority order is descending");

  console.log("\n--- once is removed ---");
  log.length = 0;
  const second = bus.emit("user:created", { id: 2, name: "Grace" });
  console.log("delivered:", second, "| log:", log.join(" | "));
  console.log("listeners now:", bus.listenerCount("user:created"));
  check(second === 2, "the once listener should be gone");

  console.log("\n--- unsubscribe ---");
  console.log("unsubscribe():", unsubscribe());
  log.length = 0;
  const third = bus.emit("user:created", { id: 3, name: "Linus" });
  console.log("delivered:", third, "| log:", log.join(" | "));
  console.log("off unknown handler:", bus.off("user:deleted", (payload) => console.log(payload.id)));
  check(third === 1, "only the low-priority listener should remain");

  console.log("\n--- independent event types ---");
  bus.on("order:placed", (payload) => console.log(`  order ${payload.orderId} total ${payload.total}`));
  bus.on("user:deleted", (payload) => console.log(`  deleted user ${payload.id}`));
  bus.emit("order:placed", { orderId: "A-1", total: 42.5 });
  bus.emit("user:deleted", { id: 7 });

  console.log("\n--- event names ---");
  console.log(bus.eventNames().map(String).join(", "));
  check(bus.eventNames().length === 3, "three events have listeners");

  console.log("\n--- clear ---");
  bus.clear();
  console.log("listeners after clear:", bus.listenerCount("user:created"), "| emit:", bus.emit("user:deleted", { id: 8 }));

  console.log("\n--- async bus ---");
  const asyncBus = new TypedEventBus<AppEvents>();
  const order: string[] = [];
  asyncBus.on("order:placed", async (payload) => {
    await new Promise((resolve) => setTimeout(resolve, 5));
    order.push(`audit ${payload.orderId}`);
  });
  asyncBus.on("order:placed", (payload) => order.push(`email ${payload.orderId}`));

  asyncBus
    .emitAsync("order:placed", { orderId: "B-2", total: 10 })
    .then((count) => {
      console.log("delivered:", count, "| order:", order.join(", "));
      check(order.join(", ") === "audit B-2, email B-2", "async listeners run sequentially");
      console.log("\nAll checks passed.");
    })
    .catch((error: unknown) => {
      console.error(error);
      process.exitCode = 1;
    });
}

main();
