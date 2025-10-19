package nbu.cscb869.web.controllers;

import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.services.services.utility.contracts.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private static final Logger logger = LoggerFactory.getLogger(VisitController.class);

    private final VisitService visitService;
    private final NotificationService notificationService;

    public VisitController(VisitService visitService, NotificationService notificationService) {
        this.visitService = visitService;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<VisitViewDTO> createVisit(@Valid @RequestBody VisitCreateDTO visitCreateDTO) {
        VisitViewDTO createdVisit = visitService.create(visitCreateDTO);

        // Extract patient email from security context
        String patientEmail = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal) {
            OAuth2AuthenticatedPrincipal principal = (OAuth2AuthenticatedPrincipal) authentication.getPrincipal();
            patientEmail = principal.getAttribute("email");
        }

        if (patientEmail != null && !patientEmail.isBlank()) {
            // Orchestrate email sending after successful visit creation
            // The NotificationService.sendVisitConfirmation is now @Async, so it won't block the controller thread.
            notificationService.sendVisitConfirmation(createdVisit, patientEmail);
        } else {
            logger.warn("Could not retrieve patient email from security context for visit {}. Email confirmation will not be sent.", createdVisit.getId());
        }

        return new ResponseEntity<>(createdVisit, HttpStatus.CREATED);
    }

    // Other CRUD and query endpoints will be added here later
}
