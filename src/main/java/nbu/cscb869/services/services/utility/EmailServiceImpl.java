package nbu.cscb869.services.services.utility;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import nbu.cscb869.services.services.utility.contracts.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Generic implementation of the EmailService using Spring's JavaMailSender.
 * This can be configured to work with any email provider (like SendGrid or a local SMTP server like MailHog)
 * through application properties.
 */
@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    // This injects a custom property for the "from" email address.
    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** {@inheritDoc} */
    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Successfully sent simple text email to {}", to);
        } catch (Exception e) {
            logger.error("Error sending simple text email to {}: {}", to, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sendHtmlMessage(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            logger.info("Successfully sent HTML email to {}", to);
        } catch (MessagingException e) {
            logger.error("Error sending HTML email to {}: {}", to, e.getMessage());
        }
    }
}
