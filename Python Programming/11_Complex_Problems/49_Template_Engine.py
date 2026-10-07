"""
Program 49: A Small Template Engine

Renders text with {{ expressions }}, {% if %}, {% for %} and a few filters. The
expressions are parsed with the ast module and evaluated by walking the tree,
allowing only the node types a template needs. That is what keeps
{{ __import__("os").system("...") }} from being a way in: the call node is
rejected before anything is looked up, so no lookup ever happens.

The | in a template means a filter, and the parser is what makes that
unambiguous. Rather than splitting the text on |, the expression is handed to
ast.parse as it stands, where | becomes a BitOr node. The checker then treats a
BitOr as a filter application, which lets it admit a call in that one position
and nowhere else. So {{ items|length == 0 }} reads as (items|length) == 0, and
{{ open('/etc/passwd') }} is still refused.

The other two decisions worth noticing:

  * output is HTML-escaped unless the template asks for |safe, which returns a
    Markup string that the renderer leaves alone
  * a missing name or key is an Undefined value, not an error, so that
    |default can rescue it - and strict=True turns that into a hard error

Concepts: tokenising, parsing into a tree, ast.parse, a structural whitelist,
           escaping untrusted values, undefined handling.

    python 49_Template_Engine.py
"""

from __future__ import annotations

import ast
from dataclasses import dataclass, field
from html import escape
import operator
import re
from typing import Any


class TemplateError(Exception):
    """Anything wrong with the template, the expression, or the data in it."""


class Markup(str):
    """Text that is already safe for HTML, so the renderer leaves it alone."""


class Undefined:
    """Stands in for something the template asked for that is not there.

    It is falsy, renders as nothing, and quietly tolerates being indexed or
    walked into further, so a chain like user.address.city cannot explode when
    one link is missing. |default is the way to give it a value.
    """

    def __init__(self, description: str) -> None:
        self.description = description

    def __repr__(self) -> str:
        return f"Undefined({self.description})"

    def __str__(self) -> str:
        return ""

    def __bool__(self) -> bool:
        return False

    def __len__(self) -> int:
        return 0

    def __iter__(self):
        return iter(())

    # Only reached when normal lookup fails, and "description" is a real
    # attribute, so these do not recurse.
    def __getattr__(self, name: str) -> "Undefined":
        return Undefined(f"{self.description}.{name}")

    def __getitem__(self, key: Any) -> "Undefined":
        return Undefined(f"{self.description}[{key!r}]")


# ------------------------------------------------------------------- the tree
@dataclass
class Text:
    value: str


@dataclass
class Output:
    expression: str


@dataclass
class If:
    # Each branch is (condition, body); the else branch has None for a condition.
    branches: list[tuple[str | None, list["Node"]]] = field(default_factory=list)


@dataclass
class For:
    variable: str
    expression: str
    body: list["Node"] = field(default_factory=list)


Node = Text | Output | If | For


# --------------------------------------------------------------- tokenising
TOKEN = re.compile(r"\{\{.*?\}\}|\{%.*?%\}", re.DOTALL)


def tokenise(source: str) -> list[tuple[str, str]]:
    """Split the source into text, output and tag tokens."""
    tokens: list[tuple[str, str]] = []
    position = 0
    for match in TOKEN.finditer(source):
        if match.start() > position:
            tokens.append(("text", source[position:match.start()]))
        raw = match.group(0)
        tokens.append(("output" if raw.startswith("{{") else "tag", raw[2:-2]))
        position = match.end()
    if position < len(source):
        tokens.append(("text", source[position:]))
    return tokens


# ----------------------------------------------------------------- parsing
def parse(source: str) -> list[Node]:
    """Build the node tree, using a stack of open blocks for the nesting."""
    root: list[Node] = []
    blocks: list[list[Node]] = [root]

    def enclosing() -> list[list[Node]]:
        if len(blocks) < 2:
            raise TemplateError("this tag has no open block to belong to")
        return blocks

    for kind, value in tokenise(source):
        if kind == "text":
            blocks[-1].append(Text(value))

        elif kind == "output":
            if not value.strip():
                raise TemplateError("an output tag needs an expression")
            blocks[-1].append(Output(value.strip()))

        else:
            head, _, rest = value.strip().partition(" ")
            head, rest = head.strip(), rest.strip()

            if head == "if":
                if not rest:
                    raise TemplateError("{% if %} needs a condition")
                node = If(branches=[(rest, [])])
                blocks[-1].append(node)
                blocks.append(node.branches[-1][1])

            elif head in ("elif", "else"):
                siblings = enclosing()[-2]
                if not siblings or not isinstance(siblings[-1], If):
                    raise TemplateError(f"{{% {head} %}} outside an if block")
                node = siblings[-1]
                node.branches.append((rest if head == "elif" else None, []))
                blocks[-1] = node.branches[-1][1]        # the previous branch is closed

            elif head == "endif":
                if not isinstance(enclosing()[-2][-1], If):
                    raise TemplateError("{% endif %} without a matching {% if %}")
                blocks.pop()

            elif head == "for":
                variable, separator, expression = rest.partition(" in ")
                if not separator or not variable.strip() or not expression.strip():
                    raise TemplateError(f"{{% for %}} should read 'for name in expression', got {value!r}")
                node = For(variable.strip(), expression.strip(), [])
                blocks[-1].append(node)
                blocks.append(node.body)

            elif head == "endfor":
                if not isinstance(enclosing()[-2][-1], For):
                    raise TemplateError("{% endfor %} without a matching {% for %}")
                blocks.pop()

            else:
                raise TemplateError(f"unknown tag {head!r}")

    if len(blocks) != 1:
        raise TemplateError("a block was opened and never closed")
    return root


# -------------------------------------------------------------- expressions
# Node types an expression may contain. Note that ast.Call is deliberately
# absent: a call is only reachable through a filter, checked separately below.
ALLOWED = (
    ast.Expression, ast.Constant, ast.Name, ast.Attribute, ast.Subscript, ast.Load,
    ast.BinOp, ast.UnaryOp, ast.BoolOp, ast.Compare, ast.IfExp, ast.Tuple, ast.List,
    ast.Add, ast.Sub, ast.Mult, ast.Div, ast.FloorDiv, ast.Mod, ast.Pow,
    ast.USub, ast.UAdd, ast.Not, ast.And, ast.Or,
    ast.Eq, ast.NotEq, ast.Lt, ast.LtE, ast.Gt, ast.GtE, ast.In, ast.NotIn,
)


FILTERS: dict[str, Any] = {
    "upper": lambda value: str(value).upper(),
    "lower": lambda value: str(value).lower(),
    "title": lambda value: str(value).title(),
    "trim": lambda value: str(value).strip(),
    "length": lambda value: len(value),
    "sort": lambda value: sorted(value),
    "reverse": lambda value: list(reversed(value)),
    "default": lambda value, fallback="": fallback
    if isinstance(value, Undefined) or value in (None, "") else value,
    "round": lambda value, places=0: round(float(value), places),
    "join": lambda value, separator=", ": separator.join(str(item) for item in value),
    "safe": lambda value: Markup(value),
}

BINARY = {
    ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul,
    ast.Div: operator.truediv, ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod, ast.Pow: operator.pow,
}
COMPARISON = {
    ast.Eq: operator.eq, ast.NotEq: operator.ne, ast.Lt: operator.lt, ast.LtE: operator.le,
    ast.Gt: operator.gt, ast.GtE: operator.ge, ast.In: lambda a, b: a in b,
    ast.NotIn: lambda a, b: a not in b,
}


def evaluate(expression: str, context: dict[str, Any]) -> Any:
    """Parse the expression, check it against the whitelist, then walk it."""
    try:
        tree = ast.parse(expression, mode="eval")
    except SyntaxError as error:
        raise TemplateError(f"bad expression {expression!r}: {error.msg}") from error
    _check(tree.body)
    return _evaluate(tree.body, context)


def _check(node: ast.AST) -> None:
    """Reject anything that is not part of the little expression language.

    A | in a template means a filter, and it arrives here as a BitOr node. That
    is the one place a call is allowed, which is why this walks the tree itself
    rather than handing everything to a flat list of node types.
    """
    if isinstance(node, ast.BinOp) and isinstance(node.op, ast.BitOr):
        _check(node.left)
        _check_filter(node.right)
        return

    if not isinstance(node, ALLOWED):
        raise TemplateError(f"{type(node).__name__} is not allowed in an expression")
    if isinstance(node, ast.Name) and node.id.startswith("_"):
        raise TemplateError(f"the name {node.id!r} is not allowed")
    if isinstance(node, ast.Attribute) and node.attr.startswith("_"):
        raise TemplateError(f"the attribute {node.attr!r} is not allowed")

    for child in ast.iter_child_nodes(node):
        _check(child)


def _check_filter(node: ast.AST) -> None:
    """The right of a | must name a filter, called with literal arguments only."""
    if isinstance(node, ast.Call):
        name = node.func
        arguments = node.args
        if node.keywords:
            raise TemplateError("filters do not take keyword arguments")
        for argument in arguments:
            if not isinstance(argument, ast.Constant):
                raise TemplateError("filter arguments must be literals")
    else:
        name, arguments = node, []

    if not isinstance(name, ast.Name):
        raise TemplateError("the right of a | must be a filter name")
    if name.id not in FILTERS:
        raise TemplateError(f"unknown filter {name.id!r}, known ones are {sorted(FILTERS)}")


def _evaluate(node: ast.AST, context: dict[str, Any]) -> Any:
    if isinstance(node, ast.Constant):
        return node.value

    if isinstance(node, ast.Name):
        return context.get(node.id, Undefined(node.id))

    if isinstance(node, ast.Attribute):
        target = _evaluate(node.value, context)
        if isinstance(target, Undefined):
            return Undefined(f"{target.description}.{node.attr}")
        if isinstance(target, dict):                  # foo.bar also means foo["bar"]
            return target.get(node.attr, Undefined(f"{_describe(node.value, context)}.{node.attr}"))
        return getattr(target, node.attr, Undefined(f"{_describe(node.value, context)}.{node.attr}"))

    if isinstance(node, ast.Subscript):
        target = _evaluate(node.value, context)
        key = _evaluate(node.slice, context)
        if isinstance(target, Undefined):
            return Undefined(f"{target.description}[{key!r}]")
        try:
            return target[key]
        except (KeyError, IndexError, TypeError):
            return Undefined(f"{_describe(node.value, context)}[{key!r}]")

    if isinstance(node, ast.BinOp) and isinstance(node.op, ast.BitOr):
        value = _evaluate(node.left, context)
        if isinstance(node.right, ast.Call):
            name = node.right.func.id
            arguments = [_evaluate(argument, context) for argument in node.right.args]
        else:
            name, arguments = node.right.id, []
        try:
            return FILTERS[name](value, *arguments)
        except (TypeError, ValueError, AttributeError) as error:
            raise TemplateError(f"the {name} filter could not be applied: {error}") from error

    if isinstance(node, ast.BinOp):
        left = _evaluate(node.left, context)
        right = _evaluate(node.right, context)
        if isinstance(left, Undefined) or isinstance(right, Undefined):
            raise TemplateError(f"cannot do arithmetic with an undefined value in {ast.unparse(node)!r}")
        try:
            return BINARY[type(node.op)](left, right)
        except (TypeError, ZeroDivisionError) as error:
            raise TemplateError(f"{ast.unparse(node)!r} failed: {error}") from error

    if isinstance(node, ast.UnaryOp):
        value = _evaluate(node.operand, context)
        if isinstance(node.op, ast.Not):
            return not value
        if isinstance(value, Undefined):
            raise TemplateError("cannot negate an undefined value")
        return -value if isinstance(node.op, ast.USub) else +value

    if isinstance(node, ast.BoolOp):
        values = [_evaluate(value, context) for value in node.values]
        return all(values) if isinstance(node.op, ast.And) else any(values)

    if isinstance(node, ast.Compare):
        left = _evaluate(node.left, context)
        for op, comparator in zip(node.ops, node.comparators):
            right = _evaluate(comparator, context)
            if isinstance(left, Undefined) or isinstance(right, Undefined):
                return False                              # an undefined value matches nothing
            if not COMPARISON[type(op)](left, right):
                return False
            left = right
        return True

    if isinstance(node, ast.IfExp):
        chosen = node.body if _evaluate(node.test, context) else node.orelse
        return _evaluate(chosen, context)

    if isinstance(node, ast.Tuple):
        return tuple(_evaluate(item, context) for item in node.elts)

    if isinstance(node, ast.List):
        return [_evaluate(item, context) for item in node.elts]

    raise TemplateError(f"cannot evaluate {type(node).__name__}")       # pragma: no cover


def _describe(node: ast.AST, context: dict[str, Any]) -> str:
    """A readable name for the thing that was missing, for the error message."""
    if isinstance(node, ast.Name):
        return node.id
    try:
        return str(_evaluate(node, context))
    except TemplateError:
        return ast.unparse(node)


# --------------------------------------------------------------- rendering
def render(source: str, context: dict[str, Any], *, strict: bool = False) -> str:
    return render_nodes(parse(source), context, strict)


def render_nodes(nodes: list[Node], context: dict[str, Any], strict: bool) -> str:
    pieces: list[str] = []
    for node in nodes:
        if isinstance(node, Text):
            pieces.append(node.value)

        elif isinstance(node, Output):
            value = evaluate(node.expression, context)
            if isinstance(value, Undefined):
                if strict:
                    raise TemplateError(f"{value.description!r} is not defined")
                pieces.append("")
            elif isinstance(value, Markup):
                pieces.append(value)                      # already escaped by |safe
            else:
                pieces.append(escape(str(value)))

        elif isinstance(node, If):
            for condition, body in node.branches:
                if condition is None or evaluate(condition, context):
                    pieces.append(render_nodes(body, context, strict))
                    break

        elif isinstance(node, For):
            items = evaluate(node.expression, context)
            if isinstance(items, Undefined):
                if strict:
                    raise TemplateError(f"{items.description!r} is not defined")
                items = []
            materialised = list(items)
            for index, item in enumerate(materialised):
                scope = dict(context)
                scope[node.variable] = item
                scope["loop"] = {
                    "index": index + 1, "index0": index,
                    "first": index == 0, "last": index == len(materialised) - 1,
                    "length": len(materialised),
                }
                pieces.append(render_nodes(node.body, scope, strict))
    return "".join(pieces)


def main() -> None:
    # ---- plain substitution -------------------------------------------------
    assert render("Hello {{ name }}!", {"name": "world"}) == "Hello world!", "simple substitution"
    assert render("no tags here", {}) == "no tags here", "text passes straight through"
    assert render("{{ a }}{{ b }}", {"a": "1", "b": "2"}) == "12", "tags sit next to each other"
    assert render("{{ count + 1 }}", {"count": 41}) == "42", "arithmetic is allowed"
    assert render("{{ price * quantity }}", {"price": 3, "quantity": 4}) == "12", "so is multiplication"
    assert render("{{ (1 + 2) * 3 }}", {}) == "9", "with the usual precedence"
    print("substitution : plain values and arithmetic")

    # ---- values are escaped unless the template says otherwise --------------
    attack = "<script>alert('xss')</script>"
    escaped = render("{{ payload }}", {"payload": attack})
    assert "&lt;script&gt;" in escaped, f"markup is escaped: {escaped}"
    assert "<script>" not in escaped, "no raw tag survives"
    assert render("{{ payload|safe }}", {"payload": attack}) == attack, "unless |safe is asked for"
    assert render("{{ 'a & b' }}", {}) == "a &amp; b", "even literals are escaped"
    assert render("{{ grade|safe }} is {{ grade }}", {"grade": "A & B"}) == "A & B is A &amp; B", "one value, both ways"
    print(f"escaping     : {escaped}")

    # ---- attributes and subscripts ------------------------------------------
    @dataclass
    class User:
        name: str
        roles: list[str]

    context = {"user": User("Ada", ["admin", "author"]), "settings": {"theme": "dark"}}
    assert render("{{ user.name }}", context) == "Ada", "attribute access"
    assert render("{{ user.roles[0] }}", context) == "admin", "subscript access"
    assert render("{{ settings['theme'] }}", context) == "dark", "and by key"
    assert render("{{ settings.theme }}", context) == "dark", "a dot also reads a dict key"
    print("lookup       : attributes, indexes and dict keys")

    # ---- conditions ---------------------------------------------------------
    template = "{% if score >= 90 %}A{% elif score >= 80 %}B{% elif score >= 70 %}C{% else %}F{% endif %}"
    for score, expected in {95: "A", 85: "B", 75: "C", 65: "F"}.items():
        assert render(template, {"score": score}) == expected, f"{score} should be {expected}"
    assert render(template, {"score": 100}) == "A", "the first matching branch wins"
    assert render("{% if not done %}pending{% endif %}", {"done": False}) == "pending", "not"
    assert render("{% if a and b %}both{% endif %}", {"a": 1, "b": 2}) == "both", "and"
    assert render("{% if a or b %}either{% endif %}", {"a": 0, "b": 2}) == "either", "or"
    assert render("{% if 'admin' in user.roles %}yes{% endif %}", context) == "yes", "membership"
    assert render("{% if missing %}shown{% else %}not shown{% endif %}", {}) == "not shown", \
        "an undefined value is falsy rather than an error"
    print("conditions   : elif chains, not, and, or, membership and undefined")

    # a filter binds to the value it follows, tighter than the comparison does
    assert render("{% if items|length == 0 %}empty{% else %}full{% endif %}", {"items": []}) == "empty", \
        "the filter binds to items, not to the whole comparison"
    assert render("{% if items|length == 0 %}empty{% else %}full{% endif %}", {"items": [1]}) == "full"
    assert render("{% if name|length > 3 %}long{% endif %}", {"name": "Ada Lovelace"}) == "long", "worked example"
    print("conditions   : items|length == 0 reads as (items|length) == 0")

    # a call is allowed in the filter position and nowhere else
    assert render("{% if 3 in (1, 2, 3) %}found{% endif %}", {}) == "found", "tuples are allowed"
    try:
        render("{% if __import__('os') %}x{% endif %}", {})
        raise AssertionError("a condition must not be a way round the whitelist")
    except TemplateError as error:
        assert "not allowed" in str(error), error
        print(f"conditions   : refused in a condition too - {error}")

    # ---- loops --------------------------------------------------------------
    assert render("{% for x in items %}{{ x }}{% endfor %}", {"items": [1, 2, 3]}) == "123", "a simple loop"
    numbered = "{% for x in items %}{{ loop.index }}:{{ x }}{% if not loop.last %}, {% endif %}{% endfor %}"
    assert render(numbered, {"items": ["a", "b", "c"]}) == "1:a, 2:b, 3:c", render(numbered, {"items": ["a", "b", "c"]})
    assert render("{% for x in items %}{% if loop.first %}[{% endif %}{{ x }}{% if loop.last %}]{% endif %}{% endfor %}",
                  {"items": ["a", "b"]}) == "[ab]", "loop.first and loop.last"
    assert render("{% for x in items %}{{ x }}{% endfor %}", {"items": []}) == "", "an empty loop renders nothing"
    nested = "{% for row in grid %}{% for cell in row %}{{ cell }}{% endfor %};{% endfor %}"
    assert render(nested, {"grid": [[1, 2], [3, 4]]}) == "12;34;", render(nested, {"grid": [[1, 2], [3, 4]]})
    assert render("{% for k in items %}{{ loop.length }}{% endfor %}", {"items": [0, 0]}) == "22", "the length"
    assert render("{% for x in items %}{{ loop.index0 }}{% endfor %}", {"items": [9, 9]}) == "01", "zero based index"
    assert render("{% for x in missing %}{{ x }}{% endfor %}done", {}) == "done", "looping over nothing is safe"
    assert render("{% for name in names|sort %}{{ name }}{% endfor %}", {"names": ["c", "a", "b"]}) == "abc", \
        "a loop can filter the collection it walks"
    print("loops        : nesting, loop.index, loop.first, loop.last and loop.length")

    # ---- filters ------------------------------------------------------------
    assert render("{{ name|upper }}", {"name": "ada"}) == "ADA", "upper"
    assert render("{{ name|lower }}", {"name": "ADA"}) == "ada", "lower"
    assert render("{{ name|title }}", {"name": "ada lovelace"}) == "Ada Lovelace", "title"
    assert render("{{ items|length }}", {"items": [1, 2, 3]}) == "3", "length"
    assert render("{{ items|join('-') }}", {"items": ["a", "b"]}) == "a-b", "join with a separator"
    assert render("{{ items|join }}", {"items": ["a", "b"]}) == "a, b", "join defaults to a comma"
    assert render("{{ 3.14159|round(2) }}", {}) == "3.14", "round takes a literal argument"
    assert render("{{ value|upper|default('none') }}", {"value": "x"}) == "X", "filters chain left to right"
    assert render("{{ '  padded  '|trim|upper }}", {}) == "PADDED", "and the order matters"
    print("filters      : upper, lower, title, trim, length, join, round, default and chaining")

    # ---- undefined values ---------------------------------------------------
    assert render("[{{ missing }}]", {}) == "[]", "a missing name renders as nothing"
    assert render("[{{ missing }}]", {"missing": None}) == "[None]", "but an explicit None does not"
    assert render("{{ missing|default('n/a') }}", {}) == "n/a", "default rescues it"
    assert render("{{ missing|default('n/a')|upper }}", {}) == "N/A", "and the chain carries on"
    assert render("{{ user.address.city|default('unknown') }}", {"user": {}}) == "unknown", \
        "even partway through a chain"
    assert render("{{ settings.missing|default('light') }}", context) == "light", "a missing dict key too"
    assert render("{{ items[9]|default('none') }}", {"items": [1]}) == "none", "and a bad index"
    print("undefined    : renders empty, is falsy, and |default fixes it")

    for source in ("{{ missing + 1 }}", "{{ -missing }}"):
        try:
            render(source, {})
            raise AssertionError(f"{source} should not be arithmetic on nothing")
        except TemplateError as error:
            assert "undefined" in str(error), error
    print("undefined    : arithmetic on it is an error rather than a silent zero")

    try:
        render("{{ missing }}", {}, strict=True)
        raise AssertionError("strict mode should refuse an undefined value")
    except TemplateError as error:
        assert "not defined" in str(error), error
        print(f"strict       : {error}")

    # ---- one realistic template ---------------------------------------------
    page = """<h1>{{ title|title }}</h1>
<ul>
{% for item in items %}  <li>{{ item.name }}{% if item.done %} (done){% endif %}</li>
{% endfor %}</ul>
{% if items|length == 0 %}<p>Nothing to do.</p>{% endif %}"""
    output = render(page, {"title": "my list", "items": [
        {"name": "write it", "done": True}, {"name": "test it", "done": False}]})
    assert "<h1>My List</h1>" in output, output
    assert "<li>write it (done)</li>" in output, output
    assert "<li>test it</li>" in output, output
    assert "Nothing to do." not in output, "the empty message is not shown"
    assert render(page, {"title": "empty", "items": []}).count("Nothing to do.") == 1, "and it is when empty"
    print("page         : a small HTML template renders as expected")

    # ---- what the whitelist stops -------------------------------------------
    dangerous = [
        "{{ open('/etc/passwd').read() }}",
        "{{ __import__('os') }}",
        "{{ ().__class__ }}",
        "{{ user.__dict__ }}",
        "{{ [x for x in items] }}",
        "{{ lambda: 1 }}",
        "{{ items[1:2] }}",
        "{{ items.append(3) }}",
        "{{ (1).__class__.__bases__ }}",
        "{{ items|upper('an argument to a filter that takes none') }}",
        "{{ items|nosuchfilter }}",
    ]
    for source in dangerous:
        try:
            render(source, {"user": User("Ada", []), "items": [1]})
            raise AssertionError(f"{source} should have been refused")
        except TemplateError as error:
            assert ("not allowed" in str(error) or "unknown filter" in str(error)
                    or "could not be applied" in str(error)), f"{source} gave {error}"
    print(f"refused      : {len(dangerous)} expressions, including open(), __import__ and __class__")

    # the check happens before evaluation, so nothing is ever looked up
    class Spy:
        def __getattr__(self, name: str) -> str:
            raise AssertionError(f"the template reached into the object for {name!r}")

    try:
        render("{{ spy.__class__ }}", {"spy": Spy()})
        raise AssertionError("the dunder should have been refused")
    except TemplateError:
        pass
    print("refused      : the refusal happens before any lookup, not after")

    # ---- malformed templates ------------------------------------------------
    bad_templates = [
        "{% if x %}unclosed",
        "{% endif %}",
        "{% for x in items %}unclosed",
        "{% endfor %}",
        "{% for items %}{% endfor %}",
        "{% if %}x{% endif %}",
        "{% elif x %}",
        "{% nonsense %}",
        "{{ }}",
        "{{ a|nosuchfilter }}",
        "{{ a|default(b) }}",
        "{{ a + }}",
        "{{ a / 0 }}",
    ]
    for source in bad_templates:
        try:
            render(source, {"a": 1, "items": [1], "x": True})
            raise AssertionError(f"{source} should have been refused")
        except TemplateError:
            pass
    print(f"rejected     : {len(bad_templates)} malformed templates and expressions")

    # an error says what was wrong, which is the point of raising rather than returning
    for source, expected in (("{% for x in items %}oops", "never closed"),
                             ("{{ a + }}", "bad expression"),
                             ("{% nonsense %}", "unknown tag"),
                             ("{{ a|nosuchfilter }}", "unknown filter")):
        try:
            render(source, {"a": 1, "items": []})
            raise AssertionError(f"{source} should have been refused")
        except TemplateError as error:
            assert expected in str(error), f"{source} gave {error}"
    print("diagnostics  : the errors name the problem")
    print("All checks passed.")


if __name__ == "__main__":
    main()
