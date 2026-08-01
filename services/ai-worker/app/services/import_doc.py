"""Import an existing .docx/.pdf into the editor's ProseMirror/Tiptap structure so a document a
researcher already started elsewhere becomes editable in the app.

Word (.docx) preserves structure: Heading 1 (and Title) start new document sections; lower headings
become heading nodes; runs carry bold/italic; List Bullet/Number styles become lists. PDF has no
reliable structure, so it comes in as paragraphs (split on blank lines) under a single section.

Best-effort by design: unknown styles fall back to paragraphs, and embedded images are not carried
across yet (the app supports adding images directly in the editor).
"""

from __future__ import annotations

import io
import re

# Only marks that exist in the editor's schema (StarterKit) are emitted; others are dropped so the
# imported JSON always validates.
_SUPPORTED_MARKS = ("bold", "italic")


def _text_node(text: str, marks: set[str] | None = None) -> dict:
    node: dict = {"type": "text", "text": text}
    picked = sorted(m for m in (marks or set()) if m in _SUPPORTED_MARKS)
    if picked:
        node["marks"] = [{"type": m} for m in picked]
    return node


def _paragraph(runs: list[tuple[str, set[str]]]) -> dict:
    content = [_text_node(t, m) for (t, m) in runs if t]
    return {"type": "paragraph", "content": content} if content else {"type": "paragraph"}


def _docx_to_sections(data: bytes) -> tuple[str | None, list[dict]]:
    from docx import Document

    doc = Document(io.BytesIO(data))
    title = (doc.core_properties.title or "").strip() or None

    sections: list[dict] = []
    current: dict = {"heading": None, "nodes": []}

    def flush() -> None:
        nonlocal current
        if current["heading"] is not None or current["nodes"]:
            sections.append(current)
        current = {"heading": None, "nodes": []}

    for p in doc.paragraphs:
        style = (p.style.name or "").lower() if p.style is not None else ""
        text = p.text
        runs: list[tuple[str, set[str]]] = []
        for r in p.runs:
            marks: set[str] = set()
            if r.bold:
                marks.add("bold")
            if r.italic:
                marks.add("italic")
            runs.append((r.text, marks))
        if not runs and text:
            runs = [(text, set())]

        if style.startswith("heading 1") or style == "title":
            flush()
            current["heading"] = text.strip() or "Section"
        elif style.startswith("heading"):
            m = re.search(r"heading (\d)", style)
            level = min(max(int(m.group(1)), 2), 4) if m else 2
            if text.strip():
                current["nodes"].append(
                    {"type": "heading", "attrs": {"level": level}, "content": [_text_node(text.strip())]}
                )
        elif style.startswith("list"):
            list_type = "orderedList" if "number" in style else "bulletList"
            item = {"type": "listItem", "content": [_paragraph(runs)]}
            prev = current["nodes"][-1] if current["nodes"] else None
            if prev and prev.get("type") == list_type:
                prev["content"].append(item)
            else:
                current["nodes"].append({"type": list_type, "content": [item]})
        elif text.strip():
            current["nodes"].append(_paragraph(runs))

    flush()

    if not sections:
        sections = [{"heading": None, "nodes": [{"type": "paragraph"}]}]

    out: list[dict] = []
    for i, s in enumerate(sections):
        heading = s["heading"] or (title if (i == 0 and title) else "Imported content")
        nodes = s["nodes"] or [{"type": "paragraph"}]
        out.append({"heading": heading, "content": {"type": "doc", "content": nodes}})
    return title, out


def _pdf_to_sections(data: bytes) -> tuple[str | None, list[dict]]:
    from pypdf import PdfReader

    reader = PdfReader(io.BytesIO(data))
    pages = []
    for page in reader.pages:
        try:
            pages.append(page.extract_text() or "")
        except Exception:  # noqa: BLE001 - a bad page shouldn't fail the whole import
            pages.append("")
    text = "\n".join(pages)

    meta = reader.metadata or {}
    title = (getattr(meta, "title", None) or "").strip() or None

    nodes = []
    for block in re.split(r"\n\s*\n", text):
        line = " ".join(l.strip() for l in block.splitlines() if l.strip()).strip()
        if line:
            nodes.append({"type": "paragraph", "content": [_text_node(line)]})
    if not nodes:
        nodes = [{"type": "paragraph"}]

    return title, [{"heading": title or "Imported document", "content": {"type": "doc", "content": nodes}}]


def import_document(filename: str, data: bytes) -> dict:
    """Returns {title, sections:[{heading, content:<ProseMirror doc>}]}. Raises ValueError for
    unsupported file types."""
    name = (filename or "").lower()
    if name.endswith(".docx"):
        title, sections = _docx_to_sections(data)
    elif name.endswith(".pdf"):
        title, sections = _pdf_to_sections(data)
    else:
        raise ValueError("Unsupported file type (expected .docx or .pdf).")
    return {"title": title, "sections": sections}
