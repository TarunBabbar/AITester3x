"""
Page Reader — Agent 1 (Browser)

Fetches a live URL with Playwright and extracts a compact, structured
snapshot of the page: title, meta description, headings, visible text,
form fields, buttons, links and interactive elements.

The snapshot is what the Test Case Generator agent analyses, so it is
deliberately trimmed to keep LLM token usage low (slim mode by default).

Everything (timeout, headless, slim, max links) is configured via .env.
"""

import logging
import os
import re
import time
from typing import Any, Dict, List

from dotenv import load_dotenv
from playwright.sync_api import sync_playwright

load_dotenv()

log = logging.getLogger(__name__)


def _collapse(text: str) -> str:
    """Collapse runs of whitespace into single spaces and trim."""
    return re.sub(r"\s+", " ", text).strip()


def _limit(text: str, max_chars: int) -> str:
    """Truncate a string with an ellipsis marker if it exceeds max_chars."""
    if len(text) <= max_chars:
        return text
    return text[:max_chars] + "…"


class PageReader:
    """Headless browser reader that snapshots a page's structure."""

    def __init__(self) -> None:
        self.timeout_ms = int(os.getenv("PAGE_READ_TIMEOUT_MS", "30000"))
        self.headless = os.getenv("PAGE_READ_HEADLESS", "true").lower() == "true"
        self.slim = os.getenv("PAGE_READ_SLIM", "true").lower() == "true"
        self.max_links = int(os.getenv("PAGE_READ_MAX_LINKS", "40"))

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------
    def snapshot(self, url: str) -> Dict[str, Any]:
        """Open url and return its structured snapshot."""
        normalized = self._normalise_url(url)
        t0 = time.perf_counter()
        log.info("Agent 1 (Page Reader) | launching headless browser for %s", normalized)
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=self.headless)
            page = browser.new_page()
            try:
                page.goto(normalized, timeout=self.timeout_ms, wait_until="domcontentloaded")
                page.wait_for_load_state("networkidle", timeout=self.timeout_ms)

                data = self._extract(page)
                data["url"] = normalized
                log.info(
                    "Agent 1 (Page Reader) | done: title=%r inputs=%d buttons=%d links=%d | %.1fs",
                    data.get("title", ""),
                    len(data.get("inputs", [])),
                    len(data.get("buttons", [])),
                    len(data.get("links", [])),
                    time.perf_counter() - t0,
                )
                return data
            except Exception:
                log.exception("Agent 1 (Page Reader) | FAILED reading %s", normalized)
                raise
            finally:
                browser.close()

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------
    @staticmethod
    def _normalise_url(url: str) -> str:
        """Prepend https:// when the user omits the scheme."""
        url = url.strip()
        if url and not url.startswith(("http://", "https://")):
            url = "https://" + url
        return url

    def _extract(self, page: Any) -> Dict[str, Any]:
        """Pull the interesting bits out of the loaded Playwright page."""
        return {
            "title": page.title() or "",
            "meta_description": self._meta_content(page, "description"),
            "meta_keywords": self._meta_content(page, "keywords"),
            "headings": self._headings(page),
            "buttons": self._buttons(page),
            "inputs": self._inputs(page),
            "links": self._links(page),
            "text": self._body_text(page),
        }

    def _meta_content(self, page: Any, name: str) -> str:
        try:
            value = page.locator(f'meta[name="{name}"]').get_attribute("content")
            return _collapse(value or "")
        except Exception:
            return ""

    def _headings(self, page: Any) -> List[str]:
        seen: List[str] = []
        for level in range(1, 4):  # h1-h3 are enough for a snapshot
            locator = page.locator(f"h{level}")
            count = locator.count()
            for i in range(min(count, 5)):
                text = _collapse(locator.nth(i).inner_text() or "")
                if text:
                    seen.append(f"h{level}: {text}")
        return seen

    def _buttons(self, page: Any) -> List[str]:
        locator = page.locator("button, [role='button'], input[type='submit'], input[type='button']")
        count = locator.count()
        buttons: List[str] = []
        for i in range(min(count, 30)):
            text = _collapse(locator.nth(i).inner_text() or "")
            value = locator.nth(i).get_attribute("value") or ""
            label = text or value
            if label:
                buttons.append(_limit(label, 60))
        return buttons

    def _inputs(self, page: Any) -> List[Dict[str, str]]:
        locator = page.locator(
            "input, textarea, select"
        )
        count = locator.count()
        inputs: List[Dict[str, str]] = []
        for i in range(min(count, 30)):
            el = locator.nth(i)
            inputs.append(
                {
                    "type": (el.get_attribute("type") or "text"),
                    "name": (el.get_attribute("name") or ""),
                    "id": (el.get_attribute("id") or ""),
                    "placeholder": (el.get_attribute("placeholder") or ""),
                    "required": (el.get_attribute("required") is not None),
                }
            )
        return inputs

    def _links(self, page: Any) -> List[str]:
        locator = page.locator("a[href]")
        count = locator.count()
        links: List[str] = []
        for i in range(min(count, self.max_links)):
            href = locator.nth(i).get_attribute("href") or ""
            text = _collapse(locator.nth(i).inner_text() or "")
            if href and href != "#":
                links.append(f"{_limit(text, 40)} -> {_limit(href, 80)}")
        return links

    def _body_text(self, page: Any) -> str:
        """Visible page text, trimmed when slim mode is on."""
        try:
            text = _collapse(page.locator("body").inner_text() or "")
        except Exception:
            return ""
        if self.slim:
            text = _limit(text, 4000)
        return text


if __name__ == "__main__":
    import sys

    url = sys.argv[1] if len(sys.argv) > 1 else "https://example.com"
    snapshot = PageReader().snapshot(url)
    for key, value in snapshot.items():
        print(f"--- {key} ---")
        print(value if isinstance(value, str) else str(value))
        print()
