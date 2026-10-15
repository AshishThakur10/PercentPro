#!/usr/bin/env python3
"""
Checks the store listing copy against Google Play's per-field limits.

The limits are not guesswork; they are documented in Play Console Help:
  - App name          30 characters
  - Short description 80 characters
  - Full description  4000 characters
  - Release notes     500 characters
They apply to full-width and half-width characters alike, so Devanagari in the
Hindi listing counts the same way as Latin text does in the English one.

Run after editing store/listing-en.md or store/listing-hi.md. Exits non-zero if
any field is over its limit.
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIMITS = {"App name": 30, "Short description": 80, "Full description": 4000,
          "Release notes": 500}


def fields(path):
    """Pull the four copy blocks out of a listing file by their headings."""
    text = open(path, encoding="utf-8").read()
    out = {}
    for name, heading in (
        ("App name", "## App name"),
        ("Short description", "## Short description"),
        ("Full description", "## Full description"),
        ("Release notes", "## Release notes"),
    ):
        start = text.find(heading)
        if start < 0:
            out[name] = None
            continue
        body = text[start + len(heading):]
        fence = body.find("```")
        if fence < 0:
            out[name] = None
            continue
        body = body[fence + 3:]
        end = body.find("```")
        out[name] = body[:end].strip("\n")
    return out


def main():
    failed = False
    for lang in ("en", "hi"):
        path = os.path.join(ROOT, "store", f"listing-{lang}.md")
        if not os.path.exists(path):
            continue
        print(f"{os.path.relpath(path, ROOT)}")
        for name, value in fields(path).items():
            if value is None:
                print(f"  {name:<20} NOT FOUND")
                failed = True
                continue
            limit = LIMITS[name]
            n = len(value)
            ok = n <= limit
            failed |= not ok
            print(f"  {name:<20} {n:>5} / {limit:<5} {'ok' if ok else 'OVER LIMIT'}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
