interface Address {
  city: string;
  zip: string;
}

interface User {
  id: number;
  name: string;
  address: Address;
  tags: string[];
}

const user: User = {
  id: 1,
  name: "Tarun",
  address: { city: "Delhi", zip: "110001" },
  tags: ["admin", "beta"],
};

function updateAddress(current: User, patch: Partial<Address>): User {
  return { ...current, address: { ...current.address, ...patch } };
}

function addTag(current: User, tag: string): User {
  return { ...current, tags: [...current.tags, tag] };
}

const movedUser = updateAddress(user, { city: "Gurgaon" });
const taggedUser = addTag(movedUser, "paid");

console.log("original:", user.address.city, user.tags.join(","));
console.log("moved:", movedUser.address.city, "zip unchanged:", movedUser.address.zip);
console.log("tagged:", taggedUser.tags.join(","));
console.log("original not mutated:", JSON.stringify(user.tags));

const sharedTags = user.tags;
console.log("structural sharing (tags ref equal):", sharedTags === movedUser.tags);

function updateAt<T, K extends keyof T>(obj: T, key: K, updater: (value: T[K]) => T[K]): T {
  return { ...obj, [key]: updater(obj[key]) };
}

const incremented = updateAt(user, "id", (id) => id + 100);
console.log("incremented id:", incremented.id, "original id:", user.id);

const frozen = Object.freeze({ ...user, address: Object.freeze({ ...user.address }) });
console.log("frozen user:", frozen.name, frozen.address.city);
