package africa.credresearch.modules.project.domain.port;

import africa.credresearch.modules.project.domain.model.Milestone;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MilestoneRepository {

    Milestone add(Milestone milestone);

    List<Milestone> findByProject(UUID projectId);

    long countByProject(UUID projectId);

    long countByProjectAndStatus(UUID projectId, String status);

    /** Next upcoming, not-yet-completed milestone for dashboard "next milestone". */
    List<Milestone> findUpcomingByProject(UUID projectId);

    /** Pending milestones due on or before {@code asOf} (for the reminder scheduler). */
    List<Milestone> findDueForReminder(LocalDate asOf);
}
