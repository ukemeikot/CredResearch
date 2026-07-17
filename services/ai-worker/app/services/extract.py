"""Paper text + metadata extraction (Phase 5, FR-LIT-2/3). Given an uploaded PDF/DOCX, pull the
full text and best-effort bibliographic metadata (title, authors, year, DOI, journal). Extraction
is heuristic — a low-confidence flag tells the app to ask the user to confirm/correct (FR-LIT-3)."""

from __future__ import annotations

import io
import re

DOI_RE = re.compile(r"10\.\d{4,9}/[-._;()/:A-Za-z0-9]+")
YEAR_RE = re.compile(r"\b(19|20)\d{2}\b")


def _clean(s: str | None) -> str | None:
    if not s:
        return None
    s = s.strip()
    return s or None


def _extract_pdf(data: bytes) -> tuple[str, dict]:
    from pypdf import PdfReader

    reader = PdfReader(io.BytesIO(data))
    pages = []
    for page in reader.pages:
        try:
            pages.append(page.extract_text() or "")
        except Exception:  # noqa: BLE001 - a bad page shouldn't fail the whole doc
            pages.append("")
    text = "\n".join(pages).strip()
    meta = reader.metadata or {}
    info = {
        "title": _clean(getattr(meta, "title", None)),
        "authors": _clean(getattr(meta, "author", None)),
    }
    return text, info


def _extract_docx(data: bytes) -> tuple[str, dict]:
    from docx import Document

    doc = Document(io.BytesIO(data))
    text = "\n".join(p.text for p in doc.paragraphs).strip()
    props = doc.core_properties
    info = {
        "title": _clean(props.title),
        "authors": _clean(props.author),
    }
    return text, info


def extract_paper(filename: str, data: bytes) -> dict:
    """Returns extracted text + metadata + a quality flag. Never raises for a readable file type."""
    name = (filename or "").lower()
    if name.endswith(".pdf"):
        text, info = _extract_pdf(data)
    elif name.endswith(".docx"):
        text, info = _extract_docx(data)
    else:
        return {
            "text": "",
            "title": None,
            "authors": None,
            "year": None,
            "doi": None,
            "journal": None,
            "low_confidence": True,
            "reason": "Unsupported file type (expected .pdf or .docx).",
        }

    head = text[:4000]  # metadata heuristics look near the top of the document

    title = info.get("title")
    title_from_metadata = bool(title)  # embedded /Title is reliable; a first-line guess is not
    if not title:
        # First substantial line of the document is a reasonable title guess — but skip common
        # front-matter boilerplate (licence/attribution notices, arXiv stamps, page headers).
        _skip = ("abstract", "doi", "http", "arxiv", "preprint", "copyright", "license",
                 "licence", "provided proper attribution", "permission", "all rights reserved",
                 "reproduce the tables", "solely for use", "downloaded from", "www.")
        for raw in head.splitlines():
            line = raw.strip()
            low = line.lower()
            if len(line) < 8 or len(line) > 250:
                continue
            if any(low.startswith(p) or p in low for p in _skip):
                continue
            if sum(c.isdigit() for c in line) > len(line) / 2:
                continue  # mostly digits → a date/line-number, not a title
            title = line[:300]
            break

    doi_match = DOI_RE.search(text)
    doi = doi_match.group(0).rstrip(".") if doi_match else None

    year_match = YEAR_RE.search(head)
    year = int(year_match.group(0)) if year_match else None

    # Confidence is weak if there's little text, no title at all, or the title was only *guessed*
    # from the first line (embedded PDF/DOCX metadata is the only title we trust silently) — so the
    # user is prompted to review a guessed title rather than it being presented as final.
    low_confidence = len(text) < 200 or not title or not title_from_metadata

    return {
        "text": text,
        "title": title,
        "authors": info.get("authors"),
        "year": year,
        "doi": doi,
        "journal": None,
        "low_confidence": low_confidence,
        "reason": None if not low_confidence else "Please review the extracted details.",
    }
