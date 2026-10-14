type Nullable<T> = T | null;

type DeepPartial<T> = {
  [K in keyof T]?: T[K] extends object ? DeepPartial<T[K]> : T[K];
};

type Merge<A, B> = Omit<A, keyof B> & B;

type Dictionary<T> = { [key: string]: T };

interface Profile {
  id: number;
  name: string;
  address: {
    city: string;
    zip: string;
  };
}

function merge<A extends object, B extends object>(base: A, override: B): Merge<A, B> {
  return { ...base, ...override };
}

const original: Profile = {
  id: 1,
  name: "Tarun",
  address: { city: "Delhi", zip: "110001" },
};

const patch: DeepPartial<Profile> = {
  name: "Tarun Babbar",
  address: { city: "Gurgaon" },
};

const merged = merge(original, { name: patch.name ?? original.name });
console.log("merged:", merged);

const maybeName: Nullable<string> = null;
console.log("nullable value:", maybeName ?? "fallback");

const flags: Dictionary<boolean> = { darkMode: true, notifications: false };
for (const key of Object.keys(flags)) {
  console.log(`${key} = ${flags[key]}`);
}

const partial: DeepPartial<Profile> = { address: { zip: "560001" } };
console.log("deep partial:", JSON.stringify(partial));
