package africa.credresearch.modules.paper.application;

import africa.credresearch.modules.paper.domain.model.Paper;

/**
 * Renders a paper's metadata into a formatted reference string for the supported citation styles
 * (FR-LIT-7). This is a pragmatic formatter over the fields we extract; a full CSL processor
 * (exact punctuation for every source type) is a later increment.
 */
public final class ReferenceFormatter {

    private ReferenceFormatter() {}

    public static String format(Paper p, String style) {
        String authors = orElse(p.authors(), "Unknown author");
        String year = p.year() != null ? String.valueOf(p.year()) : "n.d.";
        String title = orElse(p.title(), orElse(p.filename(), "Untitled"));
        String journal = trimToNull(p.journal());
        String doi = trimToNull(p.doi());

        String s = switch (style == null ? "APA" : style.toUpperCase()) {
            case "IEEE" -> ieee(authors, year, title, journal);
            case "HARVARD" -> harvard(authors, year, title, journal);
            default -> apa(authors, year, title, journal);
        };
        if (doi != null) {
            s = s + " https://doi.org/" + doi;
        }
        return s;
    }

    private static String apa(String authors, String year, String title, String journal) {
        StringBuilder b = new StringBuilder();
        b.append(authors).append(" (").append(year).append("). ").append(title).append(".");
        if (journal != null) b.append(" ").append(journal).append(".");
        return b.toString();
    }

    private static String ieee(String authors, String year, String title, String journal) {
        StringBuilder b = new StringBuilder();
        b.append(authors).append(", \"").append(title).append(",\" ");
        if (journal != null) b.append(journal).append(", ");
        b.append(year).append(".");
        return b.toString();
    }

    private static String harvard(String authors, String year, String title, String journal) {
        StringBuilder b = new StringBuilder();
        b.append(authors).append(" (").append(year).append(") '").append(title).append("'");
        if (journal != null) b.append(", ").append(journal);
        b.append(".");
        return b.toString();
    }

    private static String orElse(String v, String fallback) {
        String t = trimToNull(v);
        return t != null ? t : fallback;
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
