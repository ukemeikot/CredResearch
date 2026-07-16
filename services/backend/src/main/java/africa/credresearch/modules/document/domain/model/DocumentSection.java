package africa.credresearch.modules.document.domain.model;

import java.util.UUID;

/** An editable section of a document; {@code content} is ProseMirror/Tiptap JSON (FR-DOC-2). */
public record DocumentSection(
        UUID id, UUID documentId, int orderIndex, String chapter, String heading,
        String content, String contentText, int version) {}
