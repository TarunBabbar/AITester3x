const assert = require("node:assert/strict");

class LinkedList {
  #head = null;
  #tail = null;
  #size = 0;

  static from(values) {
    const list = new LinkedList();
    for (const value of values) {
      list.append(value);
    }
    return list;
  }

  static mergeSorted(left, right) {
    const merged = new LinkedList();
    let a = left.#head;
    let b = right.#head;

    while (a !== null && b !== null) {
      if (a.value <= b.value) {
        merged.append(a.value);
        a = a.next;
      } else {
        merged.append(b.value);
        b = b.next;
      }
    }
    while (a !== null) {
      merged.append(a.value);
      a = a.next;
    }
    while (b !== null) {
      merged.append(b.value);
      b = b.next;
    }
    return merged;
  }

  get size() {
    return this.#size;
  }

  get head() {
    return this.#head?.value;
  }

  get tail() {
    return this.#tail?.value;
  }

  append(value) {
    const node = { value, next: null };
    if (this.#tail === null) {
      this.#head = node;
    } else {
      this.#tail.next = node;
    }
    this.#tail = node;
    this.#size += 1;
    return this;
  }

  prepend(value) {
    const node = { value, next: this.#head };
    this.#head = node;
    if (this.#tail === null) {
      this.#tail = node;
    }
    this.#size += 1;
    return this;
  }

  at(index) {
    return this.#nodeAt(index)?.value;
  }

  insertAt(index, value) {
    if (!Number.isInteger(index) || index < 0 || index > this.#size) {
      throw new RangeError(`index ${index} is outside 0..${this.#size}`);
    }
    if (index === 0) {
      return this.prepend(value);
    }
    if (index === this.#size) {
      return this.append(value);
    }

    const previous = this.#nodeAt(index - 1);
    previous.next = { value, next: previous.next };
    this.#size += 1;
    return this;
  }

  removeAt(index) {
    const value = this.at(index);
    if (index === 0) {
      this.#head = this.#head.next;
      if (this.#head === null) {
        this.#tail = null;
      }
    } else {
      const previous = this.#nodeAt(index - 1);
      previous.next = previous.next.next;
      if (previous.next === null) {
        this.#tail = previous;
      }
    }
    this.#size -= 1;
    return value;
  }

  remove(value) {
    let previous = null;
    let current = this.#head;

    while (current !== null) {
      if (Object.is(current.value, value)) {
        if (previous === null) {
          this.#head = current.next;
        } else {
          previous.next = current.next;
        }
        if (current === this.#tail) {
          this.#tail = previous;
        }
        this.#size -= 1;
        return true;
      }
      previous = current;
      current = current.next;
    }

    return false;
  }

  indexOf(value) {
    let index = 0;
    for (const current of this) {
      if (Object.is(current, value)) {
        return index;
      }
      index += 1;
    }
    return -1;
  }

  reverse() {
    let previous = null;
    let current = this.#head;
    this.#tail = this.#head;

    while (current !== null) {
      const next = current.next;
      current.next = previous;
      previous = current;
      current = next;
    }

    this.#head = previous;
    return this;
  }

  middle() {
    let slow = this.#head;
    let fast = this.#head;

    while (fast !== null && fast.next !== null) {
      slow = slow.next;
      fast = fast.next.next;
    }
    return slow?.value;
  }

  hasCycle() {
    let slow = this.#head;
    let fast = this.#head;

    while (fast !== null && fast.next !== null) {
      slow = slow.next;
      fast = fast.next.next;
      if (slow === fast) {
        return true;
      }
    }
    return false;
  }

  connectTailTo(index) {
    if (this.#tail === null) {
      throw new RangeError("cannot create a cycle in an empty list");
    }
    this.#tail.next = this.#nodeAt(index);
    return this;
  }

  toArray() {
    return [...this];
  }

  *[Symbol.iterator]() {
    let current = this.#head;
    while (current !== null) {
      yield current.value;
      current = current.next;
    }
  }

  #nodeAt(index) {
    if (!Number.isInteger(index) || index < 0 || index >= this.#size) {
      throw new RangeError(`index ${index} is outside 0..${this.#size - 1}`);
    }
    let current = this.#head;
    for (let step = 0; step < index; step += 1) {
      current = current.next;
    }
    return current;
  }
}

function main() {
  console.log("--- building a list ---");
  const list = LinkedList.from([1, 2, 3]);
  console.log("array:", list.toArray().join(" -> "), "| size:", list.size, "| head:", list.head, "| tail:", list.tail);
  assert.deepEqual(list.toArray(), [1, 2, 3]);
  assert.equal(list.size, 3);
  assert.equal(list.head, 1);
  assert.equal(list.tail, 3);

  list.prepend(0).append(4);
  console.log("after prepend/append:", list.toArray().join(" -> "));
  assert.deepEqual(list.toArray(), [0, 1, 2, 3, 4]);

  console.log("\n--- indexing ---");
  assert.equal(list.at(0), 0);
  assert.equal(list.at(4), 4);
  assert.throws(() => list.at(5), RangeError);
  assert.equal(list.indexOf(3), 3);
  assert.equal(list.indexOf(9), -1);
  console.log("at(2):", list.at(2), "| indexOf(3):", list.indexOf(3));

  console.log("\n--- inserting and removing ---");
  list.insertAt(2, 99);
  console.log("insertAt(2, 99):", list.toArray().join(" -> "));
  assert.deepEqual(list.toArray(), [0, 1, 99, 2, 3, 4]);

  assert.equal(list.removeAt(2), 99);
  assert.deepEqual(list.toArray(), [0, 1, 2, 3, 4]);

  assert.equal(list.remove(1), true);
  assert.equal(list.remove(1), false);
  console.log("after remove(1):", list.toArray().join(" -> "));
  assert.deepEqual(list.toArray(), [0, 2, 3, 4]);

  list.insertAt(list.size, 5);
  list.insertAt(0, -1);
  assert.deepEqual(list.toArray(), [-1, 0, 2, 3, 4, 5]);
  assert.throws(() => list.insertAt(99, 1), RangeError);
  assert.throws(() => list.removeAt(99), RangeError);

  console.log("\n--- removing the tail updates it ---");
  assert.equal(list.removeAt(list.size - 1), 5);
  assert.equal(list.tail, 4);
  assert.equal(list.remove(4), true);
  assert.equal(list.tail, 3);
  console.log("tail after removals:", list.tail);

  console.log("\n--- reversal ---");
  const reversed = LinkedList.from([1, 2, 3, 4]).reverse();
  console.log("reversed:", reversed.toArray().join(" -> "), "| head:", reversed.head, "| tail:", reversed.tail);
  assert.deepEqual(reversed.toArray(), [4, 3, 2, 1]);
  assert.equal(reversed.head, 4);
  assert.equal(reversed.tail, 1);
  assert.deepEqual(reversed.reverse().toArray(), [1, 2, 3, 4]);

  console.log("\n--- middle element ---");
  assert.equal(LinkedList.from([1, 2, 3]).middle(), 2);
  assert.equal(LinkedList.from([1, 2, 3, 4]).middle(), 3);
  assert.equal(new LinkedList().middle(), undefined);
  console.log("odd: 2, even: 3, empty: undefined");

  console.log("\n--- cycle detection ---");
  const looped = LinkedList.from(["a", "b", "c", "d"]);
  console.log("before:", looped.hasCycle());
  looped.connectTailTo(1);
  console.log("after connectTailTo(1):", looped.hasCycle());
  assert.equal(linkedListCycle(LinkedList.from([1, 2, 3])), false);
  assert.equal(looped.hasCycle(), true);

  function linkedListCycle(value) {
    return value.hasCycle();
  }

  console.log("\n--- merging sorted lists ---");
  const merged = LinkedList.mergeSorted(LinkedList.from([1, 4, 7]), LinkedList.from([2, 3, 8, 9]));
  console.log("merged:", merged.toArray().join(" -> "));
  assert.deepEqual(merged.toArray(), [1, 2, 3, 4, 7, 8, 9]);

  const withDuplicates = LinkedList.mergeSorted(LinkedList.from([1, 1, 5]), LinkedList.from([1, 5, 5]));
  assert.deepEqual(withDuplicates.toArray(), [1, 1, 1, 5, 5, 5]);
  assert.deepEqual(LinkedList.mergeSorted(new LinkedList(), LinkedList.from([1])).toArray(), [1]);
  assert.deepEqual(LinkedList.mergeSorted(new LinkedList(), new LinkedList()).toArray(), []);

  console.log("\n--- against a plain array ---");
  let state = 21;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  const model = new LinkedList();
  const reference = [];
  for (let step = 0; step < 2_000; step += 1) {
    const action = next() % 5;
    if (action === 0 || reference.length === 0) {
      const value = next() % 100;
      model.append(value);
      reference.push(value);
    } else if (action === 1) {
      const value = next() % 100;
      model.prepend(value);
      reference.unshift(value);
    } else if (action === 2) {
      const index = next() % reference.length;
      const value = next() % 100;
      model.insertAt(index, value);
      reference.splice(index, 0, value);
    } else if (action === 3) {
      const index = next() % reference.length;
      assert.equal(model.removeAt(index), reference.splice(index, 1)[0]);
    } else {
      const value = reference[next() % reference.length];
      const expected = reference.includes(value);
      assert.equal(model.remove(value), expected);
      const position = reference.indexOf(value);
      if (expected) {
        reference.splice(position, 1);
      }
    }

    assert.equal(model.size, reference.length, `size at step ${step}`);
    assert.deepEqual(model.toArray(), reference, `contents at step ${step}`);
    assert.equal(model.head, reference[0]);
    assert.equal(model.tail, reference[reference.length - 1]);
  }
  console.log("2000 random operations match a plain array");

  console.log("\nAll checks passed.");
}

main();
