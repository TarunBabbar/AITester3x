type EventMap = Record<string, unknown>;

type Listener<T> = (payload: T) => void;

class TypedEmitter<Events extends EventMap> {
  private listeners: { [K in keyof Events]?: Listener<Events[K]>[] } = {};

  on<K extends keyof Events>(event: K, listener: Listener<Events[K]>): void {
    const list = this.listeners[event] ?? [];
    list.push(listener);
    this.listeners[event] = list;
  }

  off<K extends keyof Events>(event: K, listener: Listener<Events[K]>): void {
    const list = this.listeners[event];
    if (!list) {
      return;
    }
    this.listeners[event] = list.filter((item) => item !== listener);
  }

  emit<K extends keyof Events>(event: K, payload: Events[K]): void {
    const list = this.listeners[event] ?? [];
    for (const listener of list) {
      listener(payload);
    }
  }

  listenerCount<K extends keyof Events>(event: K): number {
    return (this.listeners[event] ?? []).length;
  }
}

interface AppEvents {
  login: { userId: number; at: Date };
  logout: { userId: number };
  error: { message: string; code: number };
}

const emitter = new TypedEmitter<AppEvents>();

const onLogin: Listener<AppEvents["login"]> = (payload) => {
  console.log(`user ${payload.userId} logged in at ${payload.at.toISOString().slice(11, 19)}`);
};

const onError: Listener<AppEvents["error"]> = (payload) => {
  console.log(`error ${payload.code}: ${payload.message}`);
};

emitter.on("login", onLogin);
emitter.on("error", onError);

console.log("login listeners:", emitter.listenerCount("login"));

emitter.emit("login", { userId: 1, at: new Date() });
emitter.emit("error", { message: "timeout", code: 504 });

emitter.off("login", onLogin);
console.log("login listeners after off:", emitter.listenerCount("login"));
emitter.emit("login", { userId: 2, at: new Date() });
console.log("done");
