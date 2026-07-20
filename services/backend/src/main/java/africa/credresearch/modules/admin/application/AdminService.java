package africa.credresearch.modules.admin.application;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Platform-admin read models (Phase 10, FR-ADM): headline counts + recent signups + per-plan
 * distribution. Read-only aggregates via JdbcTemplate; access is restricted to PLATFORM_ADMIN at
 * the controller.
 */
@Service
public class AdminService {

    public record Stats(long users, long institutions, long projects, long documents,
                        long papers, long questionnaires, long aiRequestsThisMonth,
                        List<Map<String, Object>> usersByPlan) {}

    private final JdbcTemplate jdbc;

    public AdminService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Stats stats() {
        return new Stats(
                count("users"),
                count("institutions"),
                count("projects"),
                count("documents"),
                countIfExists("papers"),
                countIfExists("questionnaires"),
                aiThisMonth(),
                usersByPlan());
    }

    public List<Map<String, Object>> recentUsers(int limit) {
        return jdbc.queryForList(
                "SELECT id, email, full_name, status, created_at FROM users ORDER BY created_at DESC LIMIT ?",
                Math.min(limit, 200));
    }

    public List<Map<String, Object>> institutions() {
        return jdbc.queryForList(
                "SELECT id, name, country, type, status FROM institutions ORDER BY name LIMIT 500");
    }

    private long count(String table) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }

    private long countIfExists(String table) {
        try {
            return count(table);
        } catch (RuntimeException e) {
            return 0; // table may not exist in older schemas
        }
    }

    private long aiThisMonth() {
        try {
            Long n = jdbc.queryForObject(
                    "SELECT count(*) FROM ai_requests WHERE created_at >= date_trunc('month', now())", Long.class);
            return n == null ? 0 : n;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private List<Map<String, Object>> usersByPlan() {
        // Plan lives on the user's institution tier; approximate by institution count per plan if present.
        try {
            return jdbc.queryForList(
                    "SELECT COALESCE(plan_code, 'FREE') AS plan, count(*) AS count FROM users GROUP BY plan_code");
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
