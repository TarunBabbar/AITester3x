interface Context {
  logger: (message: string) => void;
  config: Record<string, string>;
}

interface Plugin {
  name: string;
  setup(ctx: Context): void;
  teardown?(): void;
}

class PluginHost {
  private plugins: Plugin[] = [];
  private setupOrder: string[] = [];

  use(plugin: Plugin): this {
    this.plugins.push(plugin);
    return this;
  }

  start(config: Record<string, string> = {}): Context {
    const ctx: Context = {
      logger: (message) => console.log(`  ${message}`),
      config,
    };

    for (const plugin of this.plugins) {
      ctx.logger(`setting up ${plugin.name}`);
      plugin.setup(ctx);
      this.setupOrder.push(plugin.name);
    }

    return ctx;
  }

  stop(): void {
    for (const plugin of [...this.plugins].reverse()) {
      plugin.teardown?.();
    }
  }

  get order(): string[] {
    return [...this.setupOrder];
  }
}

const logPlugin: Plugin = {
  name: "logger",
  setup: (ctx) => ctx.logger(`logger ready (env=${ctx.config.env})`),
};

const dbPlugin: Plugin = {
  name: "database",
  setup: function (this: Plugin, ctx) {
    ctx.logger("connecting to database");
    ctx.config.connected = "true";
  },
  teardown: () => console.log("database: connection closed"),
};

const metricsPlugin: Plugin = {
  name: "metrics",
  setup: (ctx) => ctx.logger(`metrics enabled=${ctx.config.metrics ?? "false"}`),
  teardown: () => console.log("metrics: flush"),
};

const host = new PluginHost().use(logPlugin).use(dbPlugin).use(metricsPlugin);
const context = host.start({ env: "dev", metrics: "true" });

console.log("setup order:", host.order.join(" -> "));
console.log("db connected:", context.config.connected);
console.log("--- stopping ---");
host.stop();
