for (let i = 1; i <= 5; i++) {
  console.log(`for loop count: ${i}`);
}

let countdown: number = 3;
while (countdown > 0) {
  console.log(`while countdown: ${countdown}`);
  countdown--;
}

const fruits: string[] = ["apple", "banana", "cherry"];
for (const fruit of fruits) {
  console.log(`fruit: ${fruit}`);
}

fruits.forEach((fruit, index) => {
  console.log(`forEach fruit #${index}: ${fruit}`);
});
