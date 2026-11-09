function escapeHtml(text: string): string {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function renderInline(source: string): string {
  const codes: string[] = [];

  let text = source.replace(/`([^`]+)`/g, (_match, code: string) => {
    codes.push(`<code>${escapeHtml(code)}</code>`);
    return `\u0000${codes.length - 1}\u0000`;
  });

  text = escapeHtml(text);
  text = text.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
  text = text.replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, "<em>$1</em>");
  text = text.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, '<a href="$2">$1</a>');
  text = text.replace(/\u0000(\d+)\u0000/g, (_match, index: string) => codes[Number(index)]);

  return text;
}

const HEADING = /^(#{1,6})\s+(.*)$/;
const UNORDERED = /^[-*+]\s+/;
const ORDERED = /^\d+[.)]\s+/;
const RULE = /^(-{3,}|\*{3,}|_{3,})$/;

function render(markdown: string): string {
  const lines = markdown.replaceAll("\r\n", "\n").split("\n");
  const blocks: string[] = [];
  let index = 0;

  const startsBlock = (line: string): boolean =>
    HEADING.test(line) ||
    line.startsWith("```") ||
    UNORDERED.test(line) ||
    ORDERED.test(line) ||
    line.startsWith("> ") ||
    RULE.test(line.trim());

  while (index < lines.length) {
    const line = lines[index];

    if (line.trim() === "") {
      index += 1;
      continue;
    }

    if (line.startsWith("```")) {
      const language = line.slice(3).trim();
      const code: string[] = [];
      index += 1;
      while (index < lines.length && !lines[index].startsWith("```")) {
        code.push(lines[index]);
        index += 1;
      }
      if (index < lines.length) {
        index += 1;
      }
      const attribute = language === "" ? "" : ` class="language-${escapeHtml(language)}"`;
      blocks.push(`<pre><code${attribute}>${escapeHtml(code.join("\n"))}</code></pre>`);
      continue;
    }

    const heading = HEADING.exec(line);
    if (heading !== null) {
      const level = heading[1].length;
      blocks.push(`<h${level}>${renderInline(heading[2].trim())}</h${level}>`);
      index += 1;
      continue;
    }

    if (RULE.test(line.trim())) {
      blocks.push("<hr>");
      index += 1;
      continue;
    }

    if (line.startsWith("> ")) {
      const quoted: string[] = [];
      while (index < lines.length && lines[index].startsWith("> ")) {
        quoted.push(lines[index].slice(2));
        index += 1;
      }
      blocks.push(`<blockquote>${renderInline(quoted.join(" "))}</blockquote>`);
      continue;
    }

    if (UNORDERED.test(line)) {
      const items: string[] = [];
      while (index < lines.length && UNORDERED.test(lines[index])) {
        items.push(renderInline(lines[index].replace(UNORDERED, "")));
        index += 1;
      }
      blocks.push(`<ul>\n${items.map((item) => `  <li>${item}</li>`).join("\n")}\n</ul>`);
      continue;
    }

    if (ORDERED.test(line)) {
      const items: string[] = [];
      while (index < lines.length && ORDERED.test(lines[index])) {
        items.push(renderInline(lines[index].replace(ORDERED, "")));
        index += 1;
      }
      blocks.push(`<ol>\n${items.map((item) => `  <li>${item}</li>`).join("\n")}\n</ol>`);
      continue;
    }

    const paragraph: string[] = [];
    while (index < lines.length && lines[index].trim() !== "" && !startsBlock(lines[index])) {
      paragraph.push(lines[index].trim());
      index += 1;
    }
    blocks.push(`<p>${renderInline(paragraph.join(" "))}</p>`);
  }

  return blocks.join("\n");
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- headings ---");
  check(render("# Title") === "<h1>Title</h1>", "h1");
  check(render("### Three") === "<h3>Three</h3>", "h3");
  check(render("###### Six") === "<h6>Six</h6>", "h6");
  check(render("####### Seven") === "<p>####### Seven</p>", "seven hashes is a paragraph");
  console.log("levels one through six, and no level seven");

  console.log("\n--- paragraphs and inline formatting ---");
  check(render("hello world") === "<p>hello world</p>", "plain paragraph");
  check(render("two\nlines") === "<p>two lines</p>", "lines join with a space");
  check(
    render("two\n\nlines") === "<p>two</p>\n<p>lines</p>",
    "a blank line starts a new paragraph",
  );
  check(render("**bold** and *italic*") === "<p><strong>bold</strong> and <em>italic</em></p>", "emphasis");
  check(render("`code`") === "<p><code>code</code></p>", "inline code");
  check(
    render("`a * b` and *x*") === "<p><code>a * b</code> and <em>x</em></p>",
    "markdown inside code is literal",
  );
  check(
    render("[link](https://example.com?a=1&b=2)") ===
      '<p><a href="https://example.com?a=1&amp;b=2">link</a></p>',
    "links and entity escaping",
  );
  console.log("bold, italic, code, and links render");

  console.log("\n--- escaping ---");
  check(
    render("<script>alert('x')</script>") === "<p>&lt;script&gt;alert('x')&lt;/script&gt;</p>",
    "raw html is escaped",
  );
  check(render("a & b") === "<p>a &amp; b</p>", "ampersands are escaped once");
  check(
    render('[x](https://example.com/"quoted")') ===
      '<p><a href="https://example.com/&quot;quoted&quot;">x</a></p>',
    "quotes in urls are escaped",
  );
  console.log("html and entities are neutralised");

  console.log("\n--- lists ---");
  check(
    render("- one\n- two\n- three") ===
      "<ul>\n  <li>one</li>\n  <li>two</li>\n  <li>three</li>\n</ul>",
    "unordered list",
  );
  check(
    render("1. first\n2. second") === "<ol>\n  <li>first</li>\n  <li>second</li>\n</ol>",
    "ordered list",
  );
  check(render("- **a**\n- *b*") === "<ul>\n  <li><strong>a</strong></li>\n  <li><em>b</em></li>\n</ul>", "inline formatting in items");
  console.log("ordered and unordered lists, with inline formatting");

  console.log("\n--- quotes and rules ---");
  check(render("> quoted") === "<blockquote>quoted</blockquote>", "blockquote");
  check(render("> one\n> two") === "<blockquote>one two</blockquote>", "consecutive quote lines join");
  check(render("---") === "<hr>", "horizontal rule");
  check(render("_ _ _") === "<p>_ _ _</p>", "spaced underscores are not a rule");
  console.log("blockquotes and rules render");

  console.log("\n--- fenced code ---");
  const fenced = render("```ts\nconst x = 1 < 2;\n```");
  console.log(fenced);
  check(
    fenced === '<pre><code class="language-ts">const x = 1 &lt; 2;</code></pre>',
    "fenced code is escaped and labelled",
  );
  check(render("```\nplain\n```") === "<pre><code>plain</code></pre>", "unlabelled fence");
  check(
    render("```js\n**not bold**\n```") === '<pre><code class="language-js">**not bold**</code></pre>',
    "no inline formatting inside fences",
  );
  console.log("code fences keep their content literal");

  console.log("\n--- a combined document ---");
  const document = [
    "# Notes",
    "",
    "Some **bold** text with a [link](https://example.com).",
    "",
    "## Tasks",
    "",
    "- write parser",
    "- test parser",
    "",
    "1. ship it",
    "",
    "> quoted wisdom",
    "",
    "```python",
    "print('hi <there>')",
    "```",
    "",
    "---",
    "",
    "The end.",
  ].join("\n");

  const html = render(document);
  console.log(html);
  check(html.includes("<h1>Notes</h1>"), "document heading");
  check(html.includes("<h2>Tasks</h2>"), "document subheading");
  check(html.includes("<li>write parser</li>"), "document list item");
  check(html.includes("<pre><code class=\"language-python\">print('hi &lt;there&gt;')</code></pre>"), "document code");
  check(html.includes("<hr>"), "document rule");
  check(html.includes("<p>The end.</p>"), "document closing paragraph");

  console.log("\n--- edge cases ---");
  check(render("") === "", "empty input renders nothing");
  check(render("   \n\n  ") === "", "whitespace-only input renders nothing");
  check(render("#") === "<p>#</p>", "a lone hash is text");
  check(render("- ") === "<ul>\n  <li></li>\n</ul>", "an empty list item is allowed");
  check(render("```\nunclosed") === "<pre><code>unclosed</code></pre>", "an unclosed fence runs to the end");
  console.log("empty, malformed, and unclosed inputs are handled");

  console.log("\n--- round trip sanity ---");
  const tricky = "Use `a < b` and **`not bold`** with [a & b](https://x.test/a&b).";
  const trickyHtml = render(tricky);
  console.log(trickyHtml);
  check(trickyHtml.includes("<code>a &lt; b</code>"), "inline code escaped");
  check(trickyHtml.includes("<strong><code>not bold</code></strong>"), "bold around code");
  check(trickyHtml.includes("a &amp; b</a>"), "link text escaped");

  console.log("\nAll checks passed.");
}

main();
