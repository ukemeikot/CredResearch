package africa.credresearch.modules.project.application;

import africa.credresearch.modules.project.domain.model.Milestone;
import africa.credresearch.modules.project.domain.port.MilestoneRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically scans for milestones that are due (and not yet completed) and enqueues reminders
 * (FR-PROJ-5). For now this only logs; a future phase wires it to the notification port. Kept
 * minimal and non-blocking so it never breaks startup or impacts request latency.
 */
@Component
public class MilestoneReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(MilestoneReminderScheduler.class);

    private final MilestoneRepository milestones;

    public MilestoneReminderScheduler(MilestoneRepository milestones) {
        this.milestones = milestones;
    }

    /** Runs ~hourly. fixedDelay measures from the end of the previous run, so runs never overlap. */
    @Scheduled(fixedDelayString = "${credresearch.milestones.reminder-interval:PT1H}",
            initialDelayString = "${credresearch.milestones.reminder-initial-delay:PT1M}")
    public void sendDueReminders() {
        try {
            List<Milestone> due = milestones.findDueForReminder(LocalDate.now());
            if (due.isEmpty()) {
                return;
            }
            log.info("Milestone reminder sweep: {} due milestone(s) to notify", due.size());
            for (Milestone m : due) {
                log.debug("Milestone due reminder: project={} milestone={} title='{}' due={}",
                        m.projectId(), m.id(), m.title(), m.dueDate());
                // TODO(phase-x): enqueue via NotificationPort once recipient resolution lands.
            }
        } catch (RuntimeException ex) {
            // Never let a scheduler failure cascade; log and wait for the next sweep.
            log.warn("Milestone reminder sweep failed; will retry next interval", ex);
        }
    }
}
