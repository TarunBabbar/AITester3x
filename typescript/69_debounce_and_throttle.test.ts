import { test } from "node:test";
import assert from "node:assert/strict";
import { FakeClock, debounce, throttle } from "./69_debounce_and_throttle.ts";

test("FakeClock fires due timers in chronological order", () => {
  const clock = new FakeClock();
  const order: string[] = [];

  clock.setTimeout(() => order.push("slow"), 50);
  clock.setTimeout(() => order.push("fast"), 10);
  clock.advance(100);

  assert.deepEqual(order, ["fast", "slow"]);
});

test("FakeClock does not fire timers before their delay elapses", () => {
  const clock = new FakeClock();
  const order: string[] = [];

  clock.setTimeout(() => order.push("fired"), 100);
  clock.advance(99);
  assert.deepEqual(order, []);

  clock.advance(1);
  assert.deepEqual(order, ["fired"]);
});

test("FakeClock clearTimeout cancels a pending timer", () => {
  const clock = new FakeClock();
  const order: string[] = [];

  const handle = clock.setTimeout(() => order.push("fired"), 10);
  clock.clearTimeout(handle);
  clock.advance(100);

  assert.deepEqual(order, []);
});

test("debounce waits for quiet before firing once", () => {
  const clock = new FakeClock();
  const calls: string[][] = [];
  const debounced = debounce((...args: string[]) => calls.push(args), 100, clock);

  debounced("a");
  clock.advance(40);
  debounced("b");
  clock.advance(40);
  debounced("c");

  assert.deepEqual(calls, []);

  clock.advance(99);
  assert.deepEqual(calls, []);

  clock.advance(1);
  assert.deepEqual(calls, [["c"]]);
});

test("debounce restarts the timer on every call and keeps only the last args", () => {
  const clock = new FakeClock();
  const calls: number[][] = [];
  const debounced = debounce((...args: number[]) => calls.push(args), 50, clock);

  debounced(1);
  clock.advance(30);
  debounced(2);
  clock.advance(30);
  debounced(3);

  assert.deepEqual(calls, []);

  clock.advance(50);
  assert.deepEqual(calls, [[3]]);
});

test("debounce fires again for a later burst", () => {
  const clock = new FakeClock();
  const calls: string[][] = [];
  const debounced = debounce((...args: string[]) => calls.push(args), 100, clock);

  debounced("first");
  clock.advance(100);
  debounced("second");
  clock.advance(100);

  assert.deepEqual(calls, [["first"], ["second"]]);
});

test("throttle invokes immediately on the leading edge", () => {
  const clock = new FakeClock();
  const calls: number[] = [];
  const throttled = throttle((n: number) => calls.push(n), 100, clock);

  throttled(1);

  assert.deepEqual(calls, [1]);
});

test("throttle drops calls in the window and fires the latest on the trailing edge", () => {
  const clock = new FakeClock();
  const calls: number[] = [];
  const throttled = throttle((n: number) => calls.push(n), 100, clock);

  throttled(1);
  throttled(2);
  clock.advance(30);
  throttled(3);

  assert.deepEqual(calls, [1]);

  clock.advance(70);
  assert.deepEqual(calls, [1, 3]);
});

test("throttle invokes immediately again once the window has passed", () => {
  const clock = new FakeClock();
  const calls: number[] = [];
  const throttled = throttle((n: number) => calls.push(n), 100, clock);

  throttled(1);
  clock.advance(100);
  throttled(2);

  assert.deepEqual(calls, [1, 2]);
});

test("throttle forwards variadic arguments", () => {
  const clock = new FakeClock();
  const calls: [string, number][] = [];
  const throttled = throttle((label: string, value: number) => calls.push([label, value]), 100, clock);

  throttled("x", 1);
  throttled("y", 2);
  clock.advance(100);

  assert.deepEqual(calls, [["x", 1], ["y", 2]]);
});
