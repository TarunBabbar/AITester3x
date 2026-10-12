class QueryBuilder {
  private conditions: string[] = [];
  private limitValue: number | null = null;
  private orderValue: string | null = null;

  constructor(private table: string) {}

  where(condition: string): this {
    this.conditions.push(condition);
    return this;
  }

  limit(value: number): this {
    this.limitValue = value;
    return this;
  }

  orderBy(column: string): this {
    this.orderValue = column;
    return this;
  }

  build(): string {
    let query = `SELECT * FROM ${this.table}`;
    if (this.conditions.length > 0) {
      query += ` WHERE ${this.conditions.join(" AND ")}`;
    }
    if (this.orderValue) {
      query += ` ORDER BY ${this.orderValue}`;
    }
    if (this.limitValue !== null) {
      query += ` LIMIT ${this.limitValue}`;
    }
    return query;
  }
}

class UserQueryBuilder extends QueryBuilder {
  constructor() {
    super("users");
  }

  activeOnly(): this {
    return this.where("active = true");
  }
}

const query = new QueryBuilder("orders")
  .where("status = 'paid'")
  .where("total > 100")
  .orderBy("created_at")
  .limit(10)
  .build();

console.log("query:", query);

const userQuery = new UserQueryBuilder().activeOnly().orderBy("name").limit(5).build();
console.log("user query:", userQuery);
