interface Book {
  title: string;
  author: string;
  year: number;
}

const book: Book = { title: "Clean Code", author: "Robert C. Martin", year: 2008 };

const json: string = JSON.stringify(book);
console.log("json:", json);

const pretty: string = JSON.stringify(book, null, 2);
console.log("pretty:\n" + pretty);

const parsed = JSON.parse(json) as Book;
console.log("parsed title:", parsed.title);

const filterJson = JSON.stringify(book, ["title", "year"]);
console.log("filtered:", filterJson);

const withReviver = JSON.parse(
  '{"title":"Refactoring","author":"Martin Fowler","year":1999}',
  (key, value) => (key === "year" ? value + 1 : value),
) as Book;
console.log("reviver year:", withReviver.year);

function safeParse<T>(text: string): T | null {
  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}

console.log("safeParse ok:", safeParse<Book>(json)?.title);
console.log("safeParse bad:", safeParse<Book>("not json"));
