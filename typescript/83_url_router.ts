type SegmentKind = "static" | "param" | "wildcard";

interface Segment {
  kind: SegmentKind;
  value: string;
}

interface Route {
  method: string;
  pattern: string;
  segments: Segment[];
  handler: string;
  order: number;
}

interface Match {
  handler: string;
  route: string;
  params: Record<string, string>;
  score: number;
}

function compile(pattern: string): Segment[] {
  if (!pattern.startsWith("/")) {
    throw new Error(`route must start with "/": ${pattern}`);
  }

  const raw = pattern.split("/").filter((part) => part.length > 0);
  const segments: Segment[] = [];

  raw.forEach((part, index) => {
    if (part === "*") {
      if (index !== raw.length - 1) {
        throw new Error(`wildcard must be the last segment: ${pattern}`);
      }
      segments.push({ kind: "wildcard", value: "*" });
      return;
    }
    if (part.startsWith(":")) {
      const name = part.slice(1);
      if (name.length === 0) {
        throw new Error(`empty parameter name: ${pattern}`);
      }
      segments.push({ kind: "param", value: name });
      return;
    }
    segments.push({ kind: "static", value: part });
  });

  return segments;
}

function matchRoute(route: Route, parts: string[]): Match | null {
  const params: Record<string, string> = {};
  let score = 0;
  let position = 0;

  for (const segment of route.segments) {
    if (segment.kind === "wildcard") {
      params.rest = parts.slice(position).join("/");
      score += 1;
      position = parts.length;
      continue;
    }

    if (position >= parts.length) {
      return null;
    }

    const part = parts[position];
    if (segment.kind === "static") {
      if (part !== segment.value) {
        return null;
      }
      score += 4;
    } else {
      params[segment.value] = part;
      score += 2;
    }
    position += 1;
  }

  if (position !== parts.length) {
    return null;
  }

  return { handler: route.handler, route: route.pattern, params, score };
}

class Router {
  private readonly routes: Route[] = [];
  private nextOrder = 0;

  add(method: string, pattern: string, handler: string): this {
    const normalized = method.toUpperCase();
    const segments = compile(pattern);

    if (this.routes.some((route) => route.method === normalized && route.pattern === pattern)) {
      throw new Error(`duplicate route ${normalized} ${pattern}`);
    }

    this.routes.push({
      method: normalized,
      pattern,
      segments,
      handler,
      order: this.nextOrder,
    });
    this.nextOrder += 1;
    return this;
  }

  match(method: string, url: string): Match | null {
    const normalized = method.toUpperCase();
    const path = url.split("?")[0].split("#")[0];
    const parts = path
      .split("/")
      .filter((part) => part.length > 0)
      .map((part) => decodeURIComponent(part));

    let best: Match | null = null;
    let bestOrder = Number.POSITIVE_INFINITY;

    for (const route of this.routes) {
      if (route.method !== normalized && route.method !== "ANY") {
        continue;
      }
      const result = matchRoute(route, parts);
      if (result === null) {
        continue;
      }
      if (best === null || result.score > best.score || (result.score === best.score && route.order < bestOrder)) {
        best = result;
        bestOrder = route.order;
      }
    }

    return best;
  }

  get size(): number {
    return this.routes.length;
  }

  patterns(): string[] {
    return this.routes.map((route) => `${route.method} ${route.pattern}`);
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- registering routes ---");
  const router = new Router();
  router
    .add("GET", "/users", "listUsers")
    .add("GET", "/users/me", "currentUser")
    .add("GET", "/users/:id", "getUser")
    .add("POST", "/users", "createUser")
    .add("GET", "/orgs/:org/repos/:repo", "getRepo")
    .add("GET", "/files/*", "serveFile")
    .add("ANY", "/health", "health")
    .add("GET", "/", "home");

  console.log("routes:", router.size);
  check(router.size === 8, "eight routes");

  console.log("\n--- static and parameterised matches ---");
  const list = router.match("GET", "/users");
  console.log("GET /users ->", list);
  check(list?.handler === "listUsers", "static list route");
  check(Object.keys(list!.params).length === 0, "no params");

  const me = router.match("GET", "/users/me");
  console.log("GET /users/me ->", me?.handler);
  check(me?.handler === "currentUser", "static beats param");

  const user = router.match("GET", "/users/42");
  console.log("GET /users/42 ->", user);
  check(user?.handler === "getUser", "param route");
  check(user!.params.id === "42", "id captured");

  const repo = router.match("GET", "/orgs/acme/repos/engine");
  console.log("GET /orgs/acme/repos/engine ->", repo?.params);
  check(repo?.handler === "getRepo", "two parameters");
  check(repo!.params.org === "acme" && repo!.params.repo === "engine", "both captured");

  console.log("\n--- methods ---");
  const created = router.match("POST", "/users");
  console.log("POST /users ->", created?.handler);
  check(created?.handler === "createUser", "post route");
  check(router.match("DELETE", "/users") === null, "unregistered method misses");

  const health = router.match("PATCH", "/health");
  console.log("PATCH /health ->", health?.handler);
  check(health?.handler === "health", "ANY matches every method");

  console.log("\n--- wildcards ---");
  const file = router.match("GET", "/files/css/site/main.css");
  console.log("GET /files/css/site/main.css ->", file?.params.rest);
  check(file?.handler === "serveFile", "wildcard handler");
  check(file!.params.rest === "css/site/main.css", "rest captured with slashes");

  const bare = router.match("GET", "/files");
  check(bare?.handler === "serveFile" && bare.params.rest === "", "wildcard can match nothing");

  console.log("\n--- ranking ---");
  const ranking = new Router();
  ranking
    .add("GET", "/:section/:id", "catchTwoParams")
    .add("GET", "/users/:id", "usersById")
    .add("GET", "/users/*", "usersWildcard")
    .add("GET", "/users/me", "usersMe")
    .add("GET", "/files/*", "filesWildcard");

  const cases: Array<[string, string]> = [
    ["/users/me", "usersMe"],
    ["/users/7", "usersById"],
    ["/users/7/extra", "usersWildcard"],
    ["/files/a/b", "filesWildcard"],
    ["/teams/7", "catchTwoParams"],
  ];
  for (const [path, expected] of cases) {
    const match = ranking.match("GET", path);
    console.log(`  ${path} -> ${match?.handler} (score ${match?.score})`);
    check(match?.handler === expected, `${path} should hit ${expected}`);
  }

  console.log("\n--- equal scores keep registration order ---");
  const ties = new Router();
  ties.add("GET", "/a/:first", "first").add("GET", "/a/:second", "second");
  const tie = ties.match("GET", "/a/1");
  console.log("tie winner:", tie?.handler);
  check(tie?.handler === "first", "the earlier route wins");

  console.log("\n--- path normalisation ---");
  check(router.match("GET", "/users/")?.handler === "listUsers", "trailing slash ignored");
  check(router.match("GET", "/users?page=2")?.handler === "listUsers", "query ignored");
  check(router.match("GET", "/users#top")?.handler === "listUsers", "fragment ignored");
  check(router.match("GET", "/")?.handler === "home", "root route");
  check(router.match("GET", "")?.handler === "home", "empty path is the root");
  check(router.match("GET", "/USERS") === null, "paths are case sensitive");
  console.log("trailing slashes, queries, fragments, root, and case all handled");

  console.log("\n--- decoding ---");
  const searchRouter = new Router().add("GET", "/search/:term", "search");
  const encoded = searchRouter.match("GET", "/search/hello%20world");
  console.log("decoded term:", encoded?.params.term);
  check(encoded?.params.term === "hello world", "percent-encoded segments are decoded");

  console.log("\n--- invalid routes ---");
  const invalid: Array<() => void> = [
    () => new Router().add("GET", "users", "noSlash"),
    () => new Router().add("GET", "/files/*/more", "wildcardNotLast"),
    () => new Router().add("GET", "/users/:", "emptyParam"),
    () => {
      const duplicated = new Router();
      duplicated.add("GET", "/users", "one");
      duplicated.add("GET", "/users", "two");
    },
  ];
  for (const attempt of invalid) {
    let threw = false;
    try {
      attempt();
    } catch (error) {
      threw = true;
      console.log("  rejected:", (error as Error).message);
    }
    check(threw, "invalid route should throw");
  }

  console.log("\n--- same pattern, different methods ---");
  const methods = new Router();
  methods.add("GET", "/items/:id", "read").add("ANY", "/items/:id", "fallback");
  check(methods.match("GET", "/items/1")?.handler === "read", "exact method beats ANY");
  check(methods.match("POST", "/items/1")?.handler === "fallback", "ANY catches the rest");
  console.log("exact method preferred, ANY as fallback");

  console.log("\n--- listing ---");
  console.log(router.patterns().join("\n"));
  check(router.patterns().length === 8, "pattern list length");

  console.log("\nAll checks passed.");
}

main();
