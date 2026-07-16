package africa.credresearch.modules.document.application;

import com.fasterxml.jackson.databind.JsonNode;

/** Flattens ProseMirror/Tiptap JSON to plain text for full-text search / similarity (content_text). */
final class ProseMirrorText {

    private ProseMirrorText() {}

    static String flatten(JsonNode doc) {
        if (doc == null || doc.isNull()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        collect(doc, sb);
        return sb.toString().strip();
    }

    private static void collect(JsonNode node, StringBuilder sb) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()) {
                sb.append(text.asText());
            }
            JsonNode content = node.get("content");
            if (content != null) {
                collect(content, sb);
            }
            // Block-level nodes end with a newline so paragraphs stay separated in the flattened text.
            if (type != null && isBlock(type.asText())) {
                sb.append('\n');
            }
        } else if (node.isArray()) {
            node.forEach(child -> collect(child, sb));
        }
    }

    private static boolean isBlock(String type) {
        return switch (type) {
            case "paragraph", "heading", "blockquote", "listItem", "codeBlock" -> true;
            default -> false;
        };
    }
}
