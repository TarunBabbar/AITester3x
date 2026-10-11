const routes = {
  home: "/",
  about: "/about",
  contact: "/contact",
} as const;

type Route = (typeof routes)[keyof typeof routes];

const palette = {
  primary: "#0d6efd",
  danger: "#dc3545",
} satisfies Record<string, string>;

const config = {
  retries: 3,
  timeout: 5000,
} satisfies Record<string, number>;

const methods = ["GET", "POST", "DELETE"] as const;
type Method = (typeof methods)[number];

const current: Route = routes.about;
const method: Method = "POST";

console.log("routes:", routes);
console.log("current route:", current);
console.log("palette:", palette.primary, palette.danger);
console.log("config:", config);
console.log("methods:", methods.join(", "));
console.log("method:", method);

for (const value of methods) {
  console.log("method value:", value);
}
