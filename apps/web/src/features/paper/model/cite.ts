import type { Paper } from "@/lib/api";

/** Derive surnames from a free-text authors string ("Okafor, A. & Adeyemi, B." → ["Okafor","Adeyemi"]). */
function surnames(authors: string | null): string[] {
  if (!authors) return [];
  return authors
    .split(/\s*(?:;|&|,\s*and\s+|\band\b)\s*/i)
    .map((a) => {
      const t = a.trim();
      if (!t) return "";
      // "Surname, A." → Surname; otherwise the last word is usually the surname.
      return t.includes(",") ? t.split(",")[0].trim() : t.split(/\s+/).pop() ?? t;
    })
    .filter(Boolean);
}

/** Build an APA-style in-text citation, e.g. "(Okafor & Adeyemi, 2023)" (FR-LIT-6). */
export function inTextCitation(paper: Paper): string {
  const names = surnames(paper.authors);
  const year = paper.year != null ? String(paper.year) : "n.d.";
  let who: string;
  if (names.length === 0) {
    const t = (paper.title ?? paper.filename ?? "Untitled").split(/\s+/).slice(0, 2).join(" ");
    who = t;
  } else if (names.length === 1) {
    who = names[0];
  } else if (names.length === 2) {
    who = `${names[0]} & ${names[1]}`;
  } else {
    who = `${names[0]} et al.`;
  }
  return `(${who}, ${year})`;
}

/** A short label for the citation picker list. */
export function citationLabel(paper: Paper): string {
  return paper.title || paper.filename || "Untitled";
}
