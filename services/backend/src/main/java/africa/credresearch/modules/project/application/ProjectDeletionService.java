package africa.credresearch.modules.project.application;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permanently deletes a project and every row that belongs to it, across module boundaries
 * (documents, papers, questionnaires, reviews, AI ledger, membership/activity).
 *
 * <p>Deleting a project inherently spans modules and the schema mixes enforced foreign keys
 * (V4–V7, without {@code ON DELETE CASCADE}) with unconstrained {@code project_id} columns
 * (papers/questionnaires/reviews). Rather than rely on DB cascade — which isn't present — this
 * removes rows explicitly in child-before-parent order inside one transaction, so a partial failure
 * rolls back and nothing is orphaned. Callers must authorise (OWNER / platform admin) and tenant-
 * scope the project first; this class assumes the id is already vetted.
 */
@Service
public class ProjectDeletionService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDeletionService.class);

    private final JdbcTemplate jdbc;

    public ProjectDeletionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void deleteCascade(UUID projectId) {
        // ── Reviews (keyed off the project's documents) ──────────────────────────
        jdbc.update("DELETE FROM review_decisions WHERE review_request_id IN "
                + "(SELECT id FROM review_requests WHERE document_id IN "
                + "(SELECT id FROM documents WHERE project_id = ?))", projectId);
        jdbc.update("DELETE FROM review_comments WHERE review_request_id IN "
                + "(SELECT id FROM review_requests WHERE document_id IN "
                + "(SELECT id FROM documents WHERE project_id = ?))", projectId);
        jdbc.update("DELETE FROM review_requests WHERE document_id IN "
                + "(SELECT id FROM documents WHERE project_id = ?)", projectId);

        // ── AI-use ledger (disclosure/responses before requests; requests before documents) ──
        jdbc.update("DELETE FROM ai_disclosure_entries WHERE document_id IN "
                + "(SELECT id FROM documents WHERE project_id = ?)", projectId);
        jdbc.update("DELETE FROM ai_responses WHERE ai_request_id IN "
                + "(SELECT id FROM ai_requests WHERE project_id = ?)", projectId);

        // ── Documents subtree ────────────────────────────────────────────────────
        jdbc.update("DELETE FROM document_versions WHERE document_section_id IN "
                + "(SELECT s.id FROM document_sections s JOIN documents d ON s.document_id = d.id "
                + "WHERE d.project_id = ?)", projectId);
        jdbc.update("DELETE FROM document_sections WHERE document_id IN "
                + "(SELECT id FROM documents WHERE project_id = ?)", projectId);
        jdbc.update("DELETE FROM ai_requests WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM documents WHERE project_id = ?", projectId);

        // ── Papers / literature ──────────────────────────────────────────────────
        jdbc.update("DELETE FROM paper_chunks WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM papers WHERE project_id = ?", projectId);

        // ── Questionnaires + survey responses ────────────────────────────────────
        jdbc.update("DELETE FROM survey_answers WHERE survey_response_id IN "
                + "(SELECT r.id FROM survey_responses r JOIN survey_links l ON r.survey_link_id = l.id "
                + "WHERE l.questionnaire_id IN (SELECT id FROM questionnaires WHERE project_id = ?))", projectId);
        jdbc.update("DELETE FROM survey_responses WHERE survey_link_id IN "
                + "(SELECT id FROM survey_links WHERE questionnaire_id IN "
                + "(SELECT id FROM questionnaires WHERE project_id = ?))", projectId);
        jdbc.update("DELETE FROM survey_links WHERE questionnaire_id IN "
                + "(SELECT id FROM questionnaires WHERE project_id = ?)", projectId);
        jdbc.update("DELETE FROM questions WHERE questionnaire_id IN "
                + "(SELECT id FROM questionnaires WHERE project_id = ?)", projectId);
        jdbc.update("DELETE FROM questionnaires WHERE project_id = ?", projectId);

        // ── Project core ─────────────────────────────────────────────────────────
        jdbc.update("DELETE FROM project_activities WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM project_status_history WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM project_milestones WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM project_members WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM invitations WHERE project_id = ?", projectId);
        int removed = jdbc.update("DELETE FROM projects WHERE id = ?", projectId);
        log.info("Deleted project {} (rows removed from projects: {})", projectId, removed);
    }
}
