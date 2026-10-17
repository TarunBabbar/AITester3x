const users = [
  { id: 1, name: "Tarun", active: true },
  { id: 2, name: "Asha", active: false },
  { id: 3, name: "Ravi", active: true },
];

const activeNames = users.filter((user) => user.active).map((user) => user.name);
console.log("active names:", activeNames.join(", "));

const totalUsers = users.reduce((total, user) => total + 1, 0);
console.log("total users:", totalUsers);

const scores = { Tarun: 95, Ravi: 88 };
console.log("object entries:", Object.entries(scores));
console.log("object keys:", Object.keys(scores).join(", "));

const { name, ...rest } = users[0];
console.log("destructured name:", name, "rest:", rest);

const [first, second, ...others] = activeNames;
console.log("first:", first, "second:", second, "others:", others);

const clone = { ...users[0], role: "admin" };
console.log("spread clone:", clone);

const merged = [...new Set([1, 2, 2, 3, 3, 3])];
console.log("unique values:", merged.join(", "));

console.log("some active:", users.some((user) => user.active));
console.log("every active:", users.every((user) => user.active));
