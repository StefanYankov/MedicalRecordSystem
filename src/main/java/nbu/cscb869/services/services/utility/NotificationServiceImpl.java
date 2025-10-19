package nbu.cscb869.services.services.utility;

import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.utility.contracts.EmailService;
import nbu.cscb869.services.services.utility.contracts.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Implementation of the NotificationService.
 */
@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    public NotificationServiceImpl(EmailService emailService, TemplateEngine templateEngine) {
        this.emailService = emailService;
        this.templateEngine = templateEngine;
    }

    /** {@inheritDoc} */
    @Override
    @Async // Make this method asynchronous
    public void sendVisitConfirmation(VisitViewDTO visitViewDTO, String patientEmail) {
        try {
            if (patientEmail != null && !patientEmail.isBlank()) {
                Context context = new Context();
                context.setVariable("patientName", visitViewDTO.getPatient().getName());
                context.setVariable("doctorName", visitViewDTO.getDoctor().getName());
                context.setVariable("visitDate", visitViewDTO.getVisitDate());
                context.setVariable("visitTime", visitViewDTO.getVisitTime());

                String htmlBody = templateEngine.process("email/visit-confirmation", context);
                emailService.sendHtmlMessage(patientEmail, "Visit Confirmation for " + visitViewDTO.getVisitDate(), htmlBody);
            } else {
                logger.warn("Could not send confirmation email for visit {}: patient email is null or blank.", visitViewDTO.getId());
            }
        } catch (Exception e) {
            logger.error("An unexpected error occurred while trying to send confirmation for visit {}: {}", visitViewDTO.getId(), e.getMessage());
        }
    }
}
