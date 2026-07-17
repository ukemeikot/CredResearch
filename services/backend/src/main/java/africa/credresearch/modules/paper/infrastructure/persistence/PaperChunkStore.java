package africa.credresearch.modules.paper.infrastructure.persistence;

import africa.credresearch.common.util.UuidV7;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * pgvector-backed store for paper chunk embeddings (Phase 5, FR-LIT-8). Uses native SQL because
 * the embedding is a pgvector `vector` column (no JPA type). Vectors are passed as their textual
 * literal ("[0.1,0.2,...]") and cast to `vector`; retrieval ranks by cosine distance (`<=>`).
 */
@Repository
public class PaperChunkStore {

    /** A retrieved chunk with its source paper's display title. */
    public record RetrievedChunk(UUID paperId, String content, String sourceTitle) {}

    private final JdbcTemplate jdbc;

    public PaperChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void deleteByPaper(UUID paperId) {
        jdbc.update("DELETE FROM paper_chunks WHERE paper_id = ?", paperId);
    }

    public void insert(UUID paperId, UUID projectId, int index, String content, String vectorLiteral) {
        jdbc.update(
                "INSERT INTO paper_chunks (id, paper_id, project_id, chunk_index, content, embedding) "
                        + "VALUES (?, ?, ?, ?, ?, CAST(? AS vector))",
                UuidV7.generate(), paperId, projectId, index, content, vectorLiteral);
    }

    /** Top-k chunks in a project by cosine similarity to the query embedding. */
    public List<RetrievedChunk> search(UUID projectId, String queryVectorLiteral, int k) {
        return jdbc.query(
                "SELECT c.paper_id, c.content, COALESCE(p.title, p.filename, 'Untitled') AS source "
                        + "FROM paper_chunks c JOIN papers p ON p.id = c.paper_id "
                        + "WHERE c.project_id = ? AND c.embedding IS NOT NULL "
                        + "ORDER BY c.embedding <=> CAST(? AS vector) LIMIT ?",
                (rs, n) -> new RetrievedChunk(
                        (UUID) rs.getObject("paper_id"), rs.getString("content"), rs.getString("source")),
                projectId, queryVectorLiteral, k);
    }

    public boolean hasChunks(UUID projectId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM paper_chunks WHERE project_id = ?", Integer.class, projectId);
        return n != null && n > 0;
    }
}
