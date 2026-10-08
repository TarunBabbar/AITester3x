function grade(score: number): string {
  if (score >= 90) {
    return "A";
  } else if (score >= 75) {
    return "B";
  } else if (score >= 60) {
    return "C";
  } else {
    return "Fail";
  }
}

const scores: number[] = [95, 80, 65, 40];

for (const score of scores) {
  console.log(`Score ${score} -> Grade ${grade(score)}`);
}

const day: string = "Saturday";
const dayType: string =
  day === "Saturday" || day === "Sunday" ? "Weekend" : "Weekday";
console.log(`${day} is a ${dayType}`);
