class AppConfig {
  static readonly VERSION = "1.0.0";
  private static instance: AppConfig | null = null;
  private static created = 0;

  private constructor(public readonly name: string) {
    AppConfig.created++;
  }

  static getInstance(): AppConfig {
    if (AppConfig.instance === null) {
      AppConfig.instance = new AppConfig("default");
    }
    return AppConfig.instance;
  }

  static get createdCount(): number {
    return AppConfig.created;
  }

  describe(): string {
    return `${this.name} v${AppConfig.VERSION}`;
  }
}

const a = AppConfig.getInstance();
const b = AppConfig.getInstance();

console.log("a:", a.describe());
console.log("b:", b.describe());
console.log("same instance:", a === b);
console.log("created count:", AppConfig.createdCount);

class Counter {
  static total = 0;

  static increment(): number {
    return ++Counter.total;
  }

  static reset(): void {
    Counter.total = 0;
  }
}

Counter.increment();
Counter.increment();
console.log("counter total:", Counter.total);
Counter.reset();
console.log("counter after reset:", Counter.total);
