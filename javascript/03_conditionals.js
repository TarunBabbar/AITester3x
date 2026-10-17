function grade(score) {
  if (score >= 90) {
    return "A";
  } else if (score >= 75) {
    return "B";
  } else if (score >= 60) {
    return "C";
  }
  return "Fail";
}

const scores = [95, 80, 65, 40];

for (const score of scores) {
  console.log(`Score ${score} -> Grade ${grade(score)}`);
}

const day = "Saturday";
const dayType = day === "Saturday" || day === "Sunday" ? "Weekend" : "Weekday";
console.log(`${day} is a ${dayType}`);

const role = "editor";
switch (role) {
  case "admin":
    console.log("full access");
    break;
  case "editor":
    console.log("can edit content");
    break;
  default:
    console.log("read only");
}
