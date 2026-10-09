abstract class Employee {
  constructor(public readonly name: string) {}

  abstract monthlyPay(): number;

  describe(): string {
    return `${this.name} earns ${this.monthlyPay()} per month`;
  }
}

class FullTimeEmployee extends Employee {
  constructor(name: string, private annualSalary: number) {
    super(name);
  }

  monthlyPay(): number {
    return Math.round(this.annualSalary / 12);
  }
}

class Contractor extends Employee {
  constructor(name: string, private hourlyRate: number, private hours: number) {
    super(name);
  }

  monthlyPay(): number {
    return this.hourlyRate * this.hours;
  }
}

const employees: Employee[] = [
  new FullTimeEmployee("Tarun", 1200000),
  new Contractor("Ravi", 1500, 100),
];

for (const employee of employees) {
  console.log(employee.describe());
}
