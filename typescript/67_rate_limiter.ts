class TokenBucket {
  private tokens: number;
  private lastRefill: number;

  constructor(
    private readonly capacity: number,
    private readonly refillPerSecond: number,
    private readonly now: () => number = () => Date.now(),
  ) {
    this.tokens = capacity;
    this.lastRefill = now();
  }

  private refill(): void {
    const current = this.now();
    const elapsed = (current - this.lastRefill) / 1000;
    if (elapsed <= 0) return;
    this.tokens = Math.min(this.capacity, this.tokens + elapsed * this.refillPerSecond);
    this.lastRefill = current;
  }

  tryConsume(count = 1): boolean {
    this.refill();
    if (this.tokens >= count) {
      this.tokens -= count;
      return true;
    }
    return false;
  }

  get available(): number {
    this.refill();
    return Math.floor(this.tokens);
  }
}

const bucket = new TokenBucket(3, 1);
const results: boolean[] = [];
for (let i = 0; i < 5; i++) {
  results.push(bucket.tryConsume());
}
console.log("first 5 attempts:", results.join(", "));
console.log("available:", bucket.available);

const fakeClock = (() => {
  let time = 0;
  return {
    tick: (ms: number) => (time += ms),
    now: () => time,
  };
})();

const timed = new TokenBucket(2, 2, fakeClock.now);
console.log("consume 2:", timed.tryConsume(2));
console.log("consume again:", timed.tryConsume());
fakeClock.tick(500);
console.log("after 500ms available:", timed.available);
console.log("consume after wait:", timed.tryConsume());
fakeClock.tick(1000);
console.log("available after 1s:", timed.available);
