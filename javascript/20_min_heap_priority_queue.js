const assert = require("node:assert/strict");

class MinHeap {
  constructor(compare = (left, right) => left - right) {
    this.items = [];
    this.compare = compare;
  }

  static from(values, compare) {
    const heap = new MinHeap(compare);
    heap.items = [...values];
    for (let index = (heap.items.length >> 1) - 1; index >= 0; index -= 1) {
      heap.#siftDown(index);
    }
    return heap;
  }

  get size() {
    return this.items.length;
  }

  peek() {
    return this.items[0];
  }

  push(value) {
    this.items.push(value);
    this.#siftUp(this.items.length - 1);
  }

  pop() {
    if (this.items.length === 0) {
      return undefined;
    }
    const top = this.items[0];
    const last = this.items.pop();
    if (this.items.length > 0) {
      this.items[0] = last;
      this.#siftDown(0);
    }
    return top;
  }

  toArray() {
    return [...this.items];
  }

  drain() {
    const ordered = [];
    while (this.items.length > 0) {
      ordered.push(this.pop());
    }
    return ordered;
  }

  #siftUp(index) {
    while (index > 0) {
      const parent = (index - 1) >> 1;
      if (this.compare(this.items[index], this.items[parent]) >= 0) {
        break;
      }
      [this.items[index], this.items[parent]] = [this.items[parent], this.items[index]];
      index = parent;
    }
  }

  #siftDown(index) {
    const length = this.items.length;
    while (true) {
      const left = index * 2 + 1;
      const right = left + 1;
      let smallest = index;

      if (left < length && this.compare(this.items[left], this.items[smallest]) < 0) {
        smallest = left;
      }
      if (right < length && this.compare(this.items[right], this.items[smallest]) < 0) {
        smallest = right;
      }
      if (smallest === index) {
        break;
      }

      [this.items[index], this.items[smallest]] = [this.items[smallest], this.items[index]];
      index = smallest;
    }
  }
}

function heapSort(values, compare) {
  const heap = MinHeap.from(values, compare);
  return heap.drain();
}

function topK(values, k) {
  if (k <= 0) {
    return [];
  }

  const heap = new MinHeap();
  for (const value of values) {
    heap.push(value);
    if (heap.size > k) {
      heap.pop();
    }
  }
  return heap.drain();
}

function schedule(tasks) {
  const heap = new MinHeap((left, right) => left.priority - right.priority || left.order - right.order);
  tasks.forEach((task, order) => {
    heap.push({ ...task, order });
  });

  const finished = [];
  while (heap.size > 0) {
    finished.push(heap.pop().name);
  }
  return finished;
}

function checkHeapInvariant(heap) {
  const items = heap.items;
  for (let index = 1; index < items.length; index += 1) {
    const parent = (index - 1) >> 1;
    if (heap.compare(items[index], items[parent]) < 0) {
      return false;
    }
  }
  return true;
}

function main() {
  console.log("--- push and pop ---");
  const heap = new MinHeap();
  for (const value of [5, 3, 8, 1, 9, 2]) {
    heap.push(value);
  }
  console.log("size:", heap.size, "| peek:", heap.peek(), "| invariant:", checkHeapInvariant(heap));
  assert.equal(heap.size, 6);
  assert.equal(heap.peek(), 1);
  assert.ok(checkHeapInvariant(heap));

  const popped = heap.drain();
  console.log("popped:", popped.join(", "));
  assert.deepEqual(popped, [1, 2, 3, 5, 8, 9]);
  assert.equal(heap.size, 0);
  assert.equal(heap.pop(), undefined);

  console.log("\n--- heapify is linear and keeps the invariant ---");
  const unsorted = [9, 4, 7, 1, 2, 6, 3, 8, 5, 0];
  const built = MinHeap.from(unsorted);
  console.log("root:", built.peek(), "| invariant:", checkHeapInvariant(built));
  assert.equal(built.peek(), 0);
  assert.ok(checkHeapInvariant(built));
  assert.deepEqual(built.drain(), [...unsorted].sort((left, right) => left - right));

  console.log("\n--- custom comparator (max heap) ---");
  const maxHeap = new MinHeap((left, right) => right - left);
  for (const value of [4, 10, 1, 7]) {
    maxHeap.push(value);
  }
  console.log("max first:", maxHeap.drain().join(", "));
  assert.deepEqual(new MinHeap((a, b) => b - a).drain().length, 0);

  console.log("\n--- heap sort matches Array.sort ---");
  let state = 42;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };
  for (let trial = 0; trial < 200; trial += 1) {
    const length = next() % 60;
    const values = Array.from({ length }, () => (next() % 200) - 100);
    const expected = [...values].sort((left, right) => left - right);
    assert.deepEqual(heapSort(values, (a, b) => a - b), expected);
    assert.deepEqual(heapSort(values, (a, b) => b - a), [...values].sort((a, b) => b - a));
  }
  console.log("200 random arrays sorted both ways");

  console.log("\n--- top k ---");
  const scores = [42, 7, 99, 99, 13, 56, 88, 3, 71];
  const best = topK(scores, 3);
  console.log("top 3:", best.join(", "));
  assert.deepEqual(best, [88, 99, 99]);
  assert.deepEqual(topK(scores, 0), []);
  assert.deepEqual(topK(scores, 20), [...scores].sort((left, right) => left - right));
  assert.deepEqual(topK([], 5), []);

  for (let trial = 0; trial < 100; trial += 1) {
    const length = next() % 50;
    const values = Array.from({ length }, () => next() % 1000);
    const k = next() % 10;
    const expected = [...values].sort((left, right) => left - right).slice(-k);
    if (k === 0) {
      expected.length = 0;
    }
    assert.deepEqual(topK(values, k), expected);
  }
  console.log("100 random top-k checks match sorting");

  console.log("\n--- stable task scheduling ---");
  const tasks = [
    { name: "backup", priority: 5 },
    { name: "deploy", priority: 1 },
    { name: "lint", priority: 9 },
    { name: "tests", priority: 1 },
    { name: "docs", priority: 5 },
    { name: "review", priority: 1 },
  ];
  const order = schedule(tasks);
  console.log("order:", order.join(" -> "));
  assert.deepEqual(order, ["deploy", "tests", "review", "backup", "docs", "lint"]);

  console.log("\n--- duplicates and negatives ---");
  const duplicates = MinHeap.from([-5, 3, -5, 0, 3, -1, -5]);
  assert.deepEqual(duplicates.drain(), [-5, -5, -5, -1, 0, 3, 3]);

  console.log("\n--- objects with a comparison key ---");
  const people = MinHeap.from(
    [
      { name: "Ada", age: 36 },
      { name: "Grace", age: 45 },
      { name: "Linus", age: 25 },
      { name: "Margaret", age: 50 },
    ],
    (left, right) => left.age - right.age,
  );
  const youngest = [];
  while (people.size > 0) {
    youngest.push(people.pop().name);
  }
  console.log("youngest first:", youngest.join(", "));
  assert.deepEqual(youngest, ["Linus", "Ada", "Grace", "Margaret"]);

  console.log("\nAll checks passed.");
}

main();
