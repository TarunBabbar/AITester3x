interface HeapNode<T> {
  value: T;
  priority: number;
}

class PriorityQueue<T> {
  private heap: HeapNode<T>[] = [];

  get size(): number {
    return this.heap.length;
  }

  isEmpty(): boolean {
    return this.heap.length === 0;
  }

  push(value: T, priority: number): void {
    this.heap.push({ value, priority });
    this.bubbleUp(this.heap.length - 1);
  }

  pop(): T | undefined {
    if (this.heap.length === 0) return undefined;
    const top = this.heap[0];
    const last = this.heap.pop()!;
    if (this.heap.length > 0) {
      this.heap[0] = last;
      this.bubbleDown(0);
    }
    return top.value;
  }

  peek(): T | undefined {
    return this.heap[0]?.value;
  }

  private bubbleUp(index: number): void {
    while (index > 0) {
      const parent = Math.floor((index - 1) / 2);
      if (this.heap[parent].priority <= this.heap[index].priority) break;
      this.swap(parent, index);
      index = parent;
    }
  }

  private bubbleDown(index: number): void {
    const length = this.heap.length;
    while (true) {
      const left = 2 * index + 1;
      const right = 2 * index + 2;
      let smallest = index;
      if (left < length && this.heap[left].priority < this.heap[smallest].priority) {
        smallest = left;
      }
      if (right < length && this.heap[right].priority < this.heap[smallest].priority) {
        smallest = right;
      }
      if (smallest === index) break;
      this.swap(smallest, index);
      index = smallest;
    }
  }

  private swap(a: number, b: number): void {
    [this.heap[a], this.heap[b]] = [this.heap[b], this.heap[a]];
  }
}

const queue = new PriorityQueue<string>();
queue.push("write tests", 3);
queue.push("fix crash", 1);
queue.push("update docs", 5);
queue.push("review PR", 2);
queue.push("deploy", 4);

console.log("size:", queue.size);
console.log("next up:", queue.peek());

const order: string[] = [];
while (!queue.isEmpty()) {
  order.push(queue.pop()!);
}
console.log("drain order:", order.join(" -> "));

const numbers = new PriorityQueue<number>();
[42, 7, 19, 3, 88, 1].forEach((n) => numbers.push(n, n));
const sorted: number[] = [];
while (!numbers.isEmpty()) {
  sorted.push(numbers.pop()!);
}
console.log("sorted:", sorted.join(", "));
