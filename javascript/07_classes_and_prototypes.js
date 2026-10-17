class Animal {
  constructor(name) {
    this.name = name;
  }

  speak() {
    return `${this.name} makes a sound`;
  }

  get label() {
    return `[${this.name}]`;
  }

  static kingdom() {
    return "Animalia";
  }
}

class Dog extends Animal {
  constructor(name, breed) {
    super(name);
    this.breed = breed;
  }

  speak() {
    return `${this.name} barks (${this.breed})`;
  }
}

const animals = [new Animal("Generic"), new Dog("Rex", "Labrador")];

for (const animal of animals) {
  console.log(animal.speak());
}

const rex = animals[1];
console.log("label getter:", rex.label);
console.log("kingdom:", Animal.kingdom());
console.log("instanceof Dog:", rex instanceof Dog, "| instanceof Animal:", rex instanceof Animal);

function Legacy(name) {
  this.name = name;
}

Legacy.prototype.speak = function () {
  return `${this.name} speaks from the prototype`;
};

const legacy = new Legacy("OldStyle");
console.log(legacy.speak());
console.log("own vs prototype:", Object.keys(legacy).join(", "));
