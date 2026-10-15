interface Command<R = void> {
  readonly name: string;
  execute(): R;
  undo?(): void;
}

class CommandBus {
  private history: Command<unknown>[] = [];

  run<R>(command: Command<R>): R {
    const result = command.execute();
    this.history.push(command as Command<unknown>);
    return result;
  }

  undoLast(): string | null {
    const command = this.history.pop();
    if (!command) {
      return null;
    }
    command.undo?.();
    return command.name;
  }

  get size(): number {
    return this.history.length;
  }
}

class Counter {
  value = 0;
}

class IncrementCommand implements Command<number> {
  readonly name = "increment";

  constructor(private counter: Counter) {}

  execute(): number {
    return ++this.counter.value;
  }

  undo(): void {
    this.counter.value--;
  }
}

class ResetCommand implements Command<void> {
  readonly name = "reset";
  private previous = 0;

  constructor(private counter: Counter) {}

  execute(): void {
    this.previous = this.counter.value;
    this.counter.value = 0;
  }

  undo(): void {
    this.counter.value = this.previous;
  }
}

const counter = new Counter();
const bus = new CommandBus();

console.log("increment ->", bus.run(new IncrementCommand(counter)));
console.log("increment ->", bus.run(new IncrementCommand(counter)));
bus.run(new ResetCommand(counter));
console.log("after reset:", counter.value);

console.log("undo:", bus.undoLast(), "value:", counter.value);
console.log("undo:", bus.undoLast(), "value:", counter.value);
console.log("history size:", bus.size);
console.log("undo:", bus.undoLast(), "value:", counter.value);
