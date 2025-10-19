package nbu.cscb869.services.services.utility.contracts;

import nbu.cscb869.services.data.dtos.VisitViewDTO;
import org.springframework.scheduling.annotation.Async;

/**
 * High-level service for sending business-specific notifications.
 * This service orchestrates the content and triggering of notifications,
 * delegating the actual sending to the EmailService.
 */
public interface NotificationService {

    /**
     * Asynchronously sends a confirmation email for a newly created visit.
     * This method is designed to be called from a service after a visit is successfully persisted.
     * It should handle its own exceptions to prevent failure of the parent transaction.
     *
     * @param visitViewDTO The newly created VisitViewDTO containing all necessary details.
     * @param patientEmail The email address of the patient to send the confirmation to.
     */
    @Async
    void sendVisitConfirmation(VisitViewDTO visitViewDTO, String patientEmail);
}
