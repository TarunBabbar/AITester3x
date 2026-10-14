interface HttpClientConfig {
  baseUrl: string;
  timeout: number;
  retries: number;
  headers: Record<string, string>;
}

class HttpClientBuilder {
  private config: Partial<HttpClientConfig> = { headers: {} };

  withBaseUrl(url: string): this {
    this.config.baseUrl = url;
    return this;
  }

  withTimeout(ms: number): this {
    this.config.timeout = ms;
    return this;
  }

  withRetries(count: number): this {
    this.config.retries = count;
    return this;
  }

  withHeader(key: string, value: string): this {
    this.config.headers = { ...this.config.headers, [key]: value };
    return this;
  }

  build(): HttpClientConfig {
    if (!this.config.baseUrl) {
      throw new Error("baseUrl is required");
    }
    return {
      baseUrl: this.config.baseUrl,
      timeout: this.config.timeout ?? 30000,
      retries: this.config.retries ?? 0,
      headers: this.config.headers ?? {},
    };
  }
}

const client = new HttpClientBuilder()
  .withBaseUrl("https://api.example.com")
  .withTimeout(5000)
  .withRetries(3)
  .withHeader("Accept", "application/json")
  .withHeader("Authorization", "Bearer token")
  .build();

console.log("client config:", client);

const minimal = new HttpClientBuilder().withBaseUrl("https://minimal.example.com").build();
console.log("minimal config:", minimal);

try {
  new HttpClientBuilder().withTimeout(1000).build();
} catch (error) {
  console.log("build error:", (error as Error).message);
}

class RequestBuilder extends HttpClientBuilder {
  private path = "/";

  forPath(path: string): this {
    this.path = path;
    return this;
  }

  describe(): string {
    return `${this.path} -> ${this.build().baseUrl}`;
  }
}

console.log("request:", new RequestBuilder().withBaseUrl("https://x.dev").forPath("/users").describe());
