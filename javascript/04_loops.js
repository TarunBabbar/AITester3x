for (let i = 1; i <= 5; i++) {
  console.log(`for loop count: ${i}`);
}

let countdown = 3;
while (countdown > 0) {
  console.log(`while countdown: ${countdown}`);
  countdown--;
}

let attempts = 0;
do {
  attempts++;
  console.log(`do-while attempt: ${attempts}`);
} while (attempts < 2);

const fruits = ["apple", "banana", "cherry"];
for (const fruit of fruits) {
  console.log(`fruit: ${fruit}`);
}

const user = { name: "Tarun", age: 30 };
for (const key in user) {
  console.log(`${key}: ${user[key]}`);
}

fruits.forEach((fruit, index) => console.log(`forEach #${index}: ${fruit}`));
