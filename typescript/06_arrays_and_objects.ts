interface User {
  id: number;
  name: string;
  active: boolean;
}

const users: User[] = [
  { id: 1, name: "Tarun", active: true },
  { id: 2, name: "Asha", active: false },
  { id: 3, name: "Ravi", active: true },
];

const activeNames: string[] = users
  .filter((user) => user.active)
  .map((user) => user.name);

console.log("All users:", users);
console.log("Active names:", activeNames.join(", "));

const totalUsers: number = users.reduce((total, user) => total + 1, 0);
console.log("Total users:", totalUsers);
