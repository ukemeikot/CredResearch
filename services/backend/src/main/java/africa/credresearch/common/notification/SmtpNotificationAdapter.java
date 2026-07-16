package africa.credresearch.common.notification;

import africa.credresearch.common.config.CredResearchProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Sends HTML mail via SMTP (Resend in cloud, MailHog locally). Failures are logged, not fatal. */
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
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(props.email().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {} ({})", to, subject, e);
        }
    }
}
