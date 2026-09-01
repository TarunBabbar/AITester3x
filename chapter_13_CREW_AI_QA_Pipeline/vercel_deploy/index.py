"""Vercel entrypoint for the Jira QA Crew pipeline.

Vercel auto-detects a Flask instance named ``app`` at a root-level
``index.py`` (or app.py/main.py/server.py/wsgi.py/asgi.py). This thin module
re-exports the app built in api/index.py, so the whole pipeline package in
``src/`` is bundled into the single Vercel Python function.
"""

from api.index import app  # noqa: F401
