interface Account {
  id: number;
  owner: string;
  balance: number;
}

const account: Account = { id: 1, owner: "Tarun", balance: 5000 };

type AccountKey = keyof Account;

function getValue<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

function keys<T extends object>(obj: T): (keyof T)[] {
  return Object.keys(obj) as (keyof T)[];
}

function setValue<T, K extends keyof T>(obj: T, key: K, value: T[K]): void {
  obj[key] = value;
}

const accountKeys: AccountKey[] = ["id", "owner", "balance"];
console.log("keys:", accountKeys.join(", "));

console.log("getValue owner:", getValue(account, "owner"));
console.log("getValue balance:", getValue(account, "balance"));

setValue(account, "balance", 7500);
console.log("updated account:", account);

console.log("all keys:", keys(account).join(", "));
