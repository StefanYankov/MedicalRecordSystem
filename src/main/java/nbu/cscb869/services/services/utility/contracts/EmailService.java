package nbu.cscb869.services.services.utility.contracts;

import org.springframework.scheduling.annotation.Async;

/**
 * Low-level service for sending emails.
 * This interface abstracts the specific email provider implementation.
 */
public interface EmailService {

    /**
     * Asynchronously sends a simple text-based email.
     *
     * @param to      The recipient's email address.
     * @param subject The subject of the email.
     * @param text    The plain text body of the email.
     */
    @Async
    void sendSimpleMessage(String to, String subject, String text);

    /**
     * Asynchronously sends an HTML-based email.
     *
     * @param to      The recipient's email address.
     * @param subject The subject of the email.
     * @param htmlBody The HTML content of the email body.
     */
    @Async
    void sendHtmlMessage(String to, String subject, String htmlBody);
}
