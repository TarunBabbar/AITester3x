interface Entity {
  id: number;
}

class Repository<T extends Entity> {
  private items: T[] = [];

  add(item: T): T {
    this.items.push(item);
    return item;
  }

  findById(id: number): T | undefined {
    return this.items.find((item) => item.id === id);
  }

  all(): readonly T[] {
    return [...this.items];
  }

  remove(id: number): boolean {
    const index = this.items.findIndex((item) => item.id === id);
    if (index === -1) {
      return false;
    }
    this.items.splice(index, 1);
    return true;
  }

  count(): number {
    return this.items.length;
  }
}

interface User extends Entity {
  name: string;
}

interface Product extends Entity {
  title: string;
  price: number;
}

const users = new Repository<User>();
users.add({ id: 1, name: "Tarun" });
users.add({ id: 2, name: "Asha" });

const products = new Repository<Product>();
products.add({ id: 100, title: "Mouse", price: 1500 });
products.add({ id: 101, title: "Keyboard", price: 2500 });

console.log("user #2:", users.findById(2));
console.log("product #100:", products.findById(100));
console.log("users count:", users.count(), "products count:", products.count());

console.log("removed user #1:", users.remove(1));
console.log("users after removal:", users.all());
console.log("remove missing #99:", users.remove(99));
