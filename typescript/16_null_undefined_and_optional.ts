interface Address {
  city: string;
  zip?: string;
}

interface Person {
  name: string;
  email?: string;
  address?: Address;
}

const people: Person[] = [
  { name: "Tarun", email: "tarun@example.com", address: { city: "Delhi", zip: "110001" } },
  { name: "Asha", address: { city: "Mumbai" } },
  { name: "Ravi" },
];

for (const person of people) {
  const email = person.email ?? "no-email@example.com";
  const zip = person.address?.zip ?? "no zip";
  const city = person.address?.city ?? "no city";

  console.log(`${person.name} | email=${email} | city=${city} | zip=${zip}`);
}

function greetUser(name: string, nickname?: string): string {
  return nickname ? `Hello ${name} (${nickname})` : `Hello ${name}`;
}

console.log(greetUser("Tarun"));
console.log(greetUser("Tarun", "TB"));
