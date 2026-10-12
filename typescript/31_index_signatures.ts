interface Scores {
  [student: string]: number;
}

interface Config {
  name: string;
  retries: number;
  [extra: string]: string | number;
}

const scores: Scores = {
  Tarun: 95,
  Ravi: 88,
};

scores["Asha"] = 91;

for (const student of Object.keys(scores)) {
  console.log(`${student}: ${scores[student]}`);
}

const config: Config = {
  name: "app",
  retries: 3,
  region: "ap-south-1",
  timeout: 5000,
};

console.log("config name:", config.name);
console.log("config retries:", config.retries);
console.log("config region:", config["region"]);

function printScores(map: Scores): void {
  const total = Object.values(map).reduce((sum, value) => sum + value, 0);
  console.log("total:", total, "average:", (total / Object.keys(map).length).toFixed(2));
}

printScores(scores);
