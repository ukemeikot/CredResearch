"""Internal similarity check (Phase 9, FR-SIM). Deterministic shingle/Jaccard comparison of a
target document's paragraphs against the user's own corpus — flags repeated paragraphs and
citation risk (matched text with no nearby citation). This is an internal pre-check, NOT a
Turnitin-equivalent originality service."""

from __future__ import annotations

import re

CITATION_RE = re.compile(r"\([A-Z][A-Za-z’'\-]+(?:\s+(?:et al\.|and|&)\s+[A-Za-z’'\-]+)?,?\s*(?:19|20)\d{2}\)"
                         r"|\[\d+\]|et al\.", re.IGNORECASE)
_WORD_RE = re.compile(r"[a-z0-9]+")
SHINGLE_K = 5
MATCH_THRESHOLD = 0.30


def _shingles(text: str, k: int = SHINGLE_K) -> set[str]:
    words = _WORD_RE.findall(text.lower())
    if len(words) < k:
        return {" ".join(words)} if words else set()
    return {" ".join(words[i:i + k]) for i in range(len(words) - k + 1)}


def _jaccard(a: set[str], b: set[str]) -> float:
    if not a or not b:
        return 0.0
    inter = len(a & b)
    union = len(a | b)
    return inter / union if union else 0.0


def _paragraphs(text: str) -> list[str]:
    if not text:
        return []
    parts = re.split(r"\n{1,}", text)
    return [p.strip() for p in parts if len(p.strip()) >= 40]  # ignore very short lines


def check(target_text: str, sources: list[dict]) -> dict:
    target_paras = _paragraphs(target_text)
    if not target_paras:
        return {"overall_score": 0.0, "matches": [], "checked_paragraphs": 0,
                "disclaimer": "Internal check only — not a Turnitin-equivalent originality report."}

    # Pre-shingle every source paragraph once.
    source_paras = []
    for src in sources:
        for para in _paragraphs(src.get("text", "")):
            source_paras.append((src.get("id", ""), src.get("title", "Untitled"), para, _shingles(para)))

    matches = []
    matched_count = 0
    for tpara in target_paras:
        tsh = _shingles(tpara)
        best = (0.0, None, None, None)
        for sid, stitle, spara, ssh in source_paras:
            score = _jaccard(tsh, ssh)
            if score > best[0]:
                best = (score, sid, stitle, spara)
        if best[0] >= MATCH_THRESHOLD:
            matched_count += 1
            matches.append({
                "score": round(best[0], 3),
                "source_id": best[1],
                "source_title": best[2],
                "target_snippet": tpara[:280],
                "source_snippet": (best[3] or "")[:280],
                "citation_risk": not bool(CITATION_RE.search(tpara)),
            })

    overall = round(100.0 * matched_count / len(target_paras), 1)
    matches.sort(key=lambda m: m["score"], reverse=True)
    return {
        "overall_score": overall,
        "checked_paragraphs": len(target_paras),
        "matched_paragraphs": matched_count,
        "matches": matches[:50],
        "disclaimer": "Internal similarity pre-check against your own documents only — this is NOT a "
                      "Turnitin-equivalent originality report.",
    }
