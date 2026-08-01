package africa.credresearch.modules.document.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.common.util.UuidV7;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores + serves binary assets embedded in a document (uploaded images and in-app drawings). Kept
 * in Postgres (bytea) and served by the backend so the editor references them by URL, with no
 * separate object store to provision. Any project member may add or read a document's assets.
 */
@Service
public class DocumentAssetService {

    /** Cap uploads so a stray file can't exhaust memory/DB (images + PNG drawings are small). */
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final List<String> ALLOWED = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");

    private final JdbcTemplate jdbc;
    private final DocumentRepository documents;
    private final ProjectAccessGuard projectAccess;

    public DocumentAssetService(JdbcTemplate jdbc, DocumentRepository documents,
                                ProjectAccessGuard projectAccess) {
        this.jdbc = jdbc;
        this.documents = documents;
        this.projectAccess = projectAccess;
    }

    public record Asset(String mimeType, byte[] bytes) {}

    /** Store an asset and return its id. Caller must be a member of the document's project. */
    @Transactional
    public UUID save(UUID documentId, String mimeType, byte[] bytes) {
        requireMember(documentId);
        UUID userId = TenantContextHolder.require().userId();
        if (bytes == null || bytes.length == 0) {
            throw ApiException.badRequest("EMPTY_FILE", "The uploaded file is empty.");
        }
        if (bytes.length > MAX_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Images must be 8 MB or smaller.");
        }
        String mime = mimeType == null ? "" : mimeType.split(";")[0].trim().toLowerCase();
        if (!ALLOWED.contains(mime)) {
            throw ApiException.badRequest("UNSUPPORTED_IMAGE",
                    "Unsupported image type. Use PNG, JPEG, GIF, WebP or SVG.");
        }
        UUID id = UuidV7.generate();
        jdbc.update("INSERT INTO document_assets (id, document_id, mime_type, bytes, created_by) "
                + "VALUES (?, ?, ?, ?, ?)", id, documentId, mime, bytes, userId);
        return id;
    }

    /** Load an asset's bytes. Caller must be a member of the document's project. */
    public Asset load(UUID documentId, UUID assetId) {
        requireMember(documentId);
        List<Asset> rows = jdbc.query(
                "SELECT mime_type, bytes FROM document_assets WHERE id = ? AND document_id = ?",
                (rs, i) -> new Asset(rs.getString("mime_type"), rs.getBytes("bytes")),
                assetId, documentId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("ASSET_NOT_FOUND", "Asset not found");
        }
        return rows.get(0);
    }

    private void requireMember(UUID documentId) {
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));
        projectAccess.requireMember(doc.projectId());
    }
}
