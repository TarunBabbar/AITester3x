declare const brand: unique symbol;

type Brand<T, B> = T & { readonly [brand]: B };

type UserId = Brand<number, "UserId">;
type OrderId = Brand<number, "OrderId">;
type Email = Brand<string, "Email">;

function createUserId(value: number): UserId {
  return value as UserId;
}

function createOrderId(value: number): OrderId {
  return value as OrderId;
}

function createEmail(value: string): Email {
  return value as Email;
}

function getUser(id: UserId): string {
  return `user-${id}`;
}

const userId = createUserId(101);
const orderId = createOrderId(5001);
const email = createEmail("tarun@example.com");

// getUser(orderId); // Error: OrderId is not assignable to UserId

function logEmail(value: Email): void {
  console.log("email ->", value);
}

console.log("user:", getUser(userId));
console.log("order:", orderId);
logEmail(email);
console.log("raw ids:", Number(userId), Number(orderId));
