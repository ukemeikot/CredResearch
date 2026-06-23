package africa.credresearch.common.notification;

/** Provider-agnostic outbound notifications (FR-X-2). Email only at Phase 1; SMS/in-app later. */
public interface NotificationPort {

    void sendEmail(String to, String subject, String body);
}
