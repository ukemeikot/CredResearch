package africa.credresearch.modules.paper.application;

import africa.credresearch.modules.paper.domain.model.Paper;
import java.util.List;

/**
 * Serialises a project's papers to BibTeX or RIS for import into reference managers
 * (Zotero/Mendeley/EndNote) — FR-LIT-9.
 */
public final class BibliographyExporter {

    private BibliographyExporter() {}

    public static String toBibtex(List<Paper> papers) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Paper p : papers) {
            String key = citeKey(p, ++n);
            sb.append("@article{").append(key).append(",\n");
            appendBib(sb, "title", p.title());
            appendBib(sb, "author", p.authors());
            if (p.year() != null) sb.append("  year = {").append(p.year()).append("},\n");
            appendBib(sb, "journal", p.journal());
            appendBib(sb, "doi", p.doi());
            // Trim the trailing comma/newline of the last field for cleanliness.
            if (sb.charAt(sb.length() - 2) == ',') sb.delete(sb.length() - 2, sb.length() - 1);
            sb.append("}\n\n");
        }
        return sb.toString();
    }

    public static String toRis(List<Paper> papers) {
        StringBuilder sb = new StringBuilder();
        for (Paper p : papers) {
            sb.append("TY  - JOUR\n");
            appendRis(sb, "TI", p.title());
            // RIS lists each author on its own AU line; split on common separators.
            if (p.authors() != null && !p.authors().isBlank()) {
                for (String a : p.authors().split("\\s*(?:;|&|,\\s*and\\s+|\\band\\b)\\s*")) {
                    appendRis(sb, "AU", a.trim());
                }
            }
            if (p.year() != null) sb.append("PY  - ").append(p.year()).append("\n");
            appendRis(sb, "JO", p.journal());
            appendRis(sb, "DO", p.doi());
            sb.append("ER  - \n\n");
        }
        return sb.toString();
    }

    private static void appendBib(StringBuilder sb, String field, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("  ").append(field).append(" = {").append(value.replace("{", "").replace("}", "")).append("},\n");
        }
    }

    private static void appendRis(StringBuilder sb, String tag, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(tag).append("  - ").append(value).append("\n");
        }
    }

    private static String citeKey(Paper p, int n) {
        String base = "ref";
        if (p.authors() != null && !p.authors().isBlank()) {
            base = p.authors().split("[\\s,;&]+")[0].replaceAll("[^A-Za-z0-9]", "");
        } else if (p.title() != null && !p.title().isBlank()) {
            base = p.title().split("\\s+")[0].replaceAll("[^A-Za-z0-9]", "");
        }
        if (base.isBlank()) base = "ref";
        return base + (p.year() != null ? String.valueOf(p.year()) : "") + "_" + n;
    }
}
