package africa.credresearch.common.notification;

import africa.credresearch.common.config.CredResearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends mail via SMTP (MailHog locally). Failures are logged, not fatal to the request flow. */
@Component
public class SmtpNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationAdapter.class);

    private final JavaMailSender mailSender;
    private final CredResearchProperties props;

    public SmtpNotificationAdapter(JavaMailSender mailSender, CredResearchProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.email().from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {} ({})", to, subject, e);
        }
    }
}
