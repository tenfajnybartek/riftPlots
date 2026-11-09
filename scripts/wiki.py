"""Validate wiki sources and export a self-contained GitHub Wiki bundle.

Uses only the Python standard library. It never publishes or deletes files.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from collections import Counter
from pathlib import Path
from urllib.parse import quote, unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "wiki"
WIKI_URL = "https://github.com/tenfajnybartek/riftPlots/wiki"
MEDIA_URL = "https://raw.githubusercontent.com/wiki/tenfajnybartek/riftPlots"
MARKDOWN_LINK = re.compile(r"(!?\[[^\]\n]*\]\()([^\s)]+)(\))")
HTML_LINK = re.compile(r'((?:src|href)=")([^"]+)(")')
FENCE = re.compile(r"^\s*(`{3,}|~{3,})")


def prose_parts(text: str):
    """Yield text segments and whether links in them are outside fenced code."""
    fence = None
    for line in text.splitlines(keepends=True):
        match = FENCE.match(line)
        if match:
            marker = match.group(1)
            if fence is None:
                fence = marker
            elif marker[0] == fence[0] and len(marker) >= len(fence):
                fence = None
            yield line, False
        else:
            yield line, fence is None


def headings(text: str) -> set[str]:
    seen = Counter()
    result = set()
    for line, prose in prose_parts(text):
        match = re.match(r"^#{1,6}\s+(.+?)\s*#*\s*$", line) if prose else None
        if not match:
            continue
        slug = re.sub(r"[^\w\s-]", "", match.group(1).lower()).replace(" ", "-")
        count = seen[slug]
        seen[slug] += 1
        result.add(f"{slug}-{count}" if count else slug)
    return result


def local_target(page: Path, url: str):
    parsed = urlsplit(url)
    if parsed.scheme or parsed.netloc:
        return None
    target = (page.parent / unquote(parsed.path)).resolve() if parsed.path else page
    if not target.is_relative_to(SOURCE.resolve()):
        raise ValueError(f"{page.name}: link leaves the wiki directory: {url}")
    if not target.is_file():
        raise ValueError(f"{page.name}: missing target: {url}")
    return target, parsed.fragment


def validate() -> dict:
    pages = sorted(SOURCE.glob("*.md"))
    if not pages or not (SOURCE / "Home.md").is_file():
        raise ValueError("Missing Home.md or wiki source pages")
    for special in ("_Sidebar.md", "_Footer.md"):
        if not (SOURCE / special).is_file():
            raise ValueError(f"Missing {special}")
    errors = []
    graph = {}
    diagrams = 0
    for page in pages:
        text = page.read_text(encoding="utf-8")
        graph[page] = set()
        if "\ufffd" in text:
            errors.append(f"{page.name}: invalid Unicode replacement character")
        fences = [FENCE.match(line) for line in text.splitlines()]
        if sum(match is not None for match in fences) % 2:
            errors.append(f"{page.name}: unbalanced code fences")
        diagrams += len(re.findall(r"^```mermaid\s*$", text, re.MULTILINE))
        for line, prose in prose_parts(text):
            if not prose:
                continue
            for pattern in (MARKDOWN_LINK, HTML_LINK):
                for match in pattern.finditer(line):
                    url = match.group(2)
                    try:
                        resolved = local_target(page, url)
                        if resolved is None:
                            continue
                        target, fragment = resolved
                        if target.suffix == ".md":
                            graph[page].add(target)
                            if fragment and unquote(fragment) not in headings(target.read_text(encoding="utf-8")):
                                errors.append(f"{page.name}: missing heading: {url}")
                    except ValueError as exception:
                        errors.append(str(exception))
    reached = set()
    pending = [SOURCE / "Home.md", SOURCE / "_Sidebar.md"]
    while pending:
        page = pending.pop()
        if page not in reached:
            reached.add(page)
            pending.extend(graph.get(page, set()) - reached)
    for page in pages:
        if not page.name.startswith("_") and page not in reached:
            errors.append(f"{page.name}: unreachable from Home or sidebar")
    if errors:
        raise ValueError("\n".join(errors))
    assets = sorted((SOURCE / "assets").glob("*.png"))
    for asset in assets:
        if not asset.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
            raise ValueError(f"Invalid PNG: {asset.name}")
    return {"pages": len([p for p in pages if not p.name.startswith("_")]),
            "navigation_files": len([p for p in pages if p.name.startswith("_")]),
            "images": len(assets), "diagrams": diagrams}


def published_link(page: Path, url: str) -> str:
    resolved = local_target(page, url)
    if resolved is None:
        return url
    target, fragment = resolved
    if target.suffix == ".md":
        result = WIKI_URL + "/" + quote(target.stem)
    else:
        result = MEDIA_URL + "/" + quote(target.relative_to(SOURCE).as_posix(), safe="/")
    return result + ("#" + fragment if fragment else "")


def render(page: Path) -> str:
    parts = []
    for line, prose in prose_parts(page.read_text(encoding="utf-8")):
        if prose:
            for pattern in (MARKDOWN_LINK, HTML_LINK):
                line = pattern.sub(lambda match: match.group(1) + published_link(page, match.group(2)) + match.group(3), line)
        parts.append(line)
    return "".join(parts)


def export(output: Path) -> dict:
    report = validate()
    output = output.resolve()
    if not output.is_relative_to((ROOT / "build").resolve()) or output == (ROOT / "build").resolve():
        raise ValueError("Export must be a subdirectory of this project's build directory")
    files = sorted(SOURCE.glob("*.md")) + sorted((SOURCE / "assets").glob("*.png"))
    expected = {path.relative_to(SOURCE) for path in files}
    if output.exists():
        extra = {path.relative_to(output) for path in output.rglob("*") if path.is_file()} - expected
        if extra:
            raise ValueError(f"Export contains unexpected old files; use an empty build subdirectory: {sorted(map(str, extra))}")
    output.mkdir(parents=True, exist_ok=True)
    for source in files:
        target = output / source.relative_to(SOURCE)
        if not target.resolve().is_relative_to(output):
            raise ValueError(f"Unsafe export path: {target}")
        target.parent.mkdir(parents=True, exist_ok=True)
        if source.suffix == ".md":
            target.write_text(render(source), encoding="utf-8")
        else:
            shutil.copyfile(source, target)
    return dict(report, output=str(output), exported_files=len(files))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("check", "export"))
    parser.add_argument("--output", type=Path, default=ROOT / "build" / "wiki-export")
    args = parser.parse_args()
    try:
        report = validate() if args.command == "check" else export(args.output)
    except (ValueError, OSError) as exception:
        print(str(exception), file=sys.stderr)
        return 1
    print(json.dumps(report, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
