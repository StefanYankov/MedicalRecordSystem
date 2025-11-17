package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * RESTful API Controller for authenticated users to retrieve their own data.
 */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Me API", description = "Endpoints for authenticated users to retrieve their own data.")
@ApiStandardResponses
public class MeApiController {
    private static final Logger logger = LoggerFactory.getLogger(MeApiController.class);
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final VisitService visitService;

    public MeApiController(PatientService patientService, DoctorService doctorService, VisitService visitService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.visitService = visitService;
    }

    @Operation(summary = "Get my dashboard", description = "Retrieves the dashboard for the currently authenticated user. " +
            "If the user is a DOCTOR, it returns their upcoming scheduled visits. " +
            "If the user is a PATIENT, it returns their profile information.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves the dashboard for the currently authenticated user.
     * If the user is a DOCTOR, it returns their upcoming scheduled visits.
     * If the user is a PATIENT, it returns their profile information.
     *
     * @param authentication The current authentication principal.
     * @param pageable Pagination information for doctor's visits.
     * @return A ResponseEntity containing the user-specific dashboard information.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getMyDashboard(Authentication authentication, @Parameter(description = "Pagination information (for doctors)") Pageable pageable) {
        if (authentication == null) {
            return new ResponseEntity<>("Authentication required.", HttpStatus.UNAUTHORIZED);
        }
        String keycloakId = authentication.getName();
        logger.info("API GET request for own dashboard for user: {}", keycloakId);

        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            DoctorViewDTO doctor = doctorService.getByKeycloakId(keycloakId);
            Page<VisitViewDTO> scheduledVisits = visitService.getVisitsByDoctorAndStatusAndDateRange(
                    doctor.getId(),
                    VisitStatus.SCHEDULED,
                    LocalDate.now(),
                    LocalDate.now().plusYears(1),
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
            return ResponseEntity.ok(scheduledVisits);
        } else { // ROLE_PATIENT
            PatientViewDTO patient = patientService.getByKeycloakId(keycloakId);
            return ResponseEntity.ok(patient);
        }
    }

    @Operation(summary = "Get my medical history (Patient)", description = "Retrieves the visit history for the currently authenticated patient.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves the visit history for the currently authenticated patient.
     *
     * @param authentication The current authentication principal.
     * @param pageable Pagination information.
     * @return A ResponseEntity containing a Page of the patient's past visits.
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Page<VisitViewDTO>> getMyHistory(Authentication authentication, @Parameter(description = "Pagination information") Pageable pageable) {
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String keycloakId = authentication.getName();
        logger.info("API GET request for own history for user: {}", keycloakId);
        PatientViewDTO patient = patientService.getByKeycloakId(keycloakId);
        Page<VisitViewDTO> history = visitService.getVisitsByPatient(patient.getId(), pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(history);
    }
}
