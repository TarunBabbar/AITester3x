const user = { name: "Tarun", age: 30, city: "Delhi" };

const { name, age, city = "Unknown" } = user;
console.log(`name=${name}, age=${age}, city=${city}`);

const { name: fullName, ...rest } = user;
console.log("fullName:", fullName);
console.log("rest:", rest);

const numbers: number[] = [1, 2, 3, 4, 5];
const [first, second, ...others] = numbers;
console.log("first:", first, "second:", second, "others:", others);

const clone = { ...user, country: "India" };
console.log("clone:", clone);

const merged = [...numbers, 6, 7];
console.log("merged:", merged);

function printUser({ name, age }: { name: string; age: number }): void {
  console.log(`${name} is ${age} years old`);
}
printUser(user);
