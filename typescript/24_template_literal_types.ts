type EventName<T extends string> = `on${Capitalize<T>}`;
type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";
type ApiPath = `/api/${string}`;
type Endpoint = `${HttpMethod} ${ApiPath}`;

type ClickEvent = EventName<"click">;
type SubmitEvent = EventName<"submit">;

const clickHandler: ClickEvent = "onClick";
const submitHandler: SubmitEvent = "onSubmit";

const getUsers: Endpoint = "GET /api/users";
const createUser: Endpoint = "POST /api/users";

function makeEventName(action: string): string {
  const capitalized = action.charAt(0).toUpperCase() + action.slice(1);
  return `on${capitalized}`;
}

const methods: HttpMethod[] = ["GET", "POST", "PUT", "DELETE"];
console.log("click handler:", clickHandler);
console.log("submit handler:", submitHandler);
console.log("endpoints:", getUsers, "|", createUser);
console.log("methods:", methods.join(", "));
console.log("dynamic event:", makeEventName("change"));
