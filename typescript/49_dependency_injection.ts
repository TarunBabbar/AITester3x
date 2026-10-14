interface Logger {
  log(message: string): void;
}

interface Clock {
  now(): Date;
}

class ConsoleLogger implements Logger {
  log(message: string): void {
    console.log(`[log] ${message}`);
  }
}

class FixedClock implements Clock {
  constructor(private fixed: Date) {}

  now(): Date {
    return this.fixed;
  }
}

class Container {
  private factories = new Map<symbol, () => unknown>();

  register<T>(token: symbol, factory: () => T): void {
    this.factories.set(token, factory);
  }

  resolve<T>(token: symbol): T {
    const factory = this.factories.get(token);
    if (!factory) {
      throw new Error("No registration for token");
    }
    return factory() as T;
  }
}

const TOKENS = {
  logger: Symbol("logger"),
  clock: Symbol("clock"),
};

class UserService {
  constructor(
    private logger: Logger,
    private clock: Clock,
  ) {}

  createUser(name: string): string {
    this.logger.log(`creating user ${name} at ${this.clock.now().toISOString()}`);
    return `user:${name}`;
  }
}

const container = new Container();
container.register(TOKENS.logger, () => new ConsoleLogger());
container.register(TOKENS.clock, () => new FixedClock(new Date("2026-01-01T00:00:00Z")));

const logger = container.resolve<Logger>(TOKENS.logger);
const clock = container.resolve<Clock>(TOKENS.clock);

const service = new UserService(logger, clock);
console.log("result:", service.createUser("Tarun"));

try {
  container.resolve(Symbol("missing"));
} catch (error) {
  console.log("resolve error:", (error as Error).message);
}
