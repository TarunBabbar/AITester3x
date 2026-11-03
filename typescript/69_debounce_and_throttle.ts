export interface Timer {
  now(): number;
  setTimeout(fn: () => void, delay: number): number;
  clearTimeout(id: number): void;
}

interface Scheduled {
  id: number;
  time: number;
  fn: () => void;
}

export class FakeClock implements Timer {
  private time = 0;
  private nextId = 1;
  private timers = new Map<number, Scheduled>();

  now(): number {
    return this.time;
  }

  setTimeout(fn: () => void, delay: number): number {
    const id = this.nextId++;
    this.timers.set(id, { id, time: this.time + delay, fn });
    return id;
  }

  clearTimeout(id: number): void {
    this.timers.delete(id);
  }

  advance(ms: number): void {
    const target = this.time + ms;
    while (true) {
      let next: Scheduled | undefined;
      for (const task of this.timers.values()) {
        if (task.time <= target && (!next || task.time < next.time)) {
          next = task;
        }
      }
      if (!next) break;
      this.timers.delete(next.id);
      this.time = next.time;
      next.fn();
    }
    this.time = target;
  }
}

export function debounce<A extends unknown[]>(
  fn: (...args: A) => void,
  wait: number,
  timer: Timer,
): (...args: A) => void {
  let handle: number | undefined;
  return (...args: A) => {
    if (handle !== undefined) {
      timer.clearTimeout(handle);
    }
    handle = timer.setTimeout(() => {
      handle = undefined;
      fn(...args);
    }, wait);
  };
}

export function throttle<A extends unknown[]>(
  fn: (...args: A) => void,
  wait: number,
  timer: Timer,
): (...args: A) => void {
  let last = -Infinity;
  let pending: A | undefined;
  let handle: number | undefined;

  const invoke = (args: A): void => {
    last = timer.now();
    fn(...args);
  };

  return (...args: A) => {
    const now = timer.now();
    const remaining = wait - (now - last);
    if (remaining <= 0) {
      if (handle !== undefined) {
        timer.clearTimeout(handle);
        handle = undefined;
      }
      pending = undefined;
      invoke(args);
    } else {
      pending = args;
      if (handle === undefined) {
        handle = timer.setTimeout(() => {
          handle = undefined;
          if (pending !== undefined) {
            const args = pending;
            pending = undefined;
            invoke(args);
          }
        }, remaining);
      }
    }
  };
}

function demo(): void {
  const clock = new FakeClock();
  const log: string[] = [];

  const debounced = debounce((value: string) => log.push(`debounced:${value}`), 100, clock);

  debounced("a");
  clock.advance(50);
  debounced("b");
  clock.advance(50);
  debounced("c");
  console.log("debounce before wait:", log.length === 0 ? "(none)" : log.join(" | "));
  clock.advance(100);
  console.log("debounce after wait:", log.join(" | "));

  const throttled = throttle((value: string) => log.push(`throttled:${value}`), 100, clock);

  log.length = 0;
  throttled("1");
  throttled("2");
  clock.advance(30);
  throttled("3");
  console.log("throttle immediate:", log.join(" | "));
  clock.advance(100);
  console.log("throttle trailing:", log.join(" | "));

  log.length = 0;
  const quiet = debounce((value: string) => log.push(`saved:${value}`), 200, clock);
  quiet("first");
  quiet("second");
  clock.advance(200);
  console.log("only last survives:", log.join(" | "));
}

const argv = (globalThis as { process?: { argv?: string[] } }).process?.argv;
if (argv?.[1] !== undefined && import.meta.url === `file://${argv[1]}`) {
  demo();
}
