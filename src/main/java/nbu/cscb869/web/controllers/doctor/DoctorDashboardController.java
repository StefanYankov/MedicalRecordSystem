package nbu.cscb869.web.controllers.doctor;

import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * Controller for handling the doctor's main dashboard.
 * Provides views for the doctor's scheduled visits.
 */
@Controller
@RequestMapping("/doctor")
public class DoctorDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorDashboardController.class);
    private final DoctorService doctorService;
    private final VisitService visitService;

    /**
     * Constructs the controller with necessary services.
     *
     * @param doctorService Service for doctor-related operations.
     * @param visitService  Service for visit-related operations.
     */
    public DoctorDashboardController(final DoctorService doctorService, final VisitService visitService) {
        this.doctorService = doctorService;
        this.visitService = visitService;
    }

    /**
     * Displays the doctor's dashboard, showing a paginated list of their scheduled visits.
     *
     * @param model          The Spring UI model.
     * @param authentication The current authentication principal.
     * @param pageable       Pagination information from the request.
     * @return The name of the dashboard view.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorDashboard(final Model model,
                                  final Authentication authentication,
                                  @PageableDefault(size = 10, sort = "visitDate") final Pageable pageable) {

        final String keycloakId = authentication.getName();
        logger.info("GET /doctor/dashboard: Displaying dashboard for user {} with page settings: {}", keycloakId, pageable);

        final DoctorViewDTO doctor = doctorService.getByKeycloakId(keycloakId);

        // Fetch scheduled visits from today onwards.
        final Page<VisitViewDTO> scheduledVisits = visitService.getVisitsByDoctorAndStatusAndDateRange(
                doctor.getId(),
                VisitStatus.SCHEDULED,
                LocalDate.now(),
                LocalDate.now().plusYears(1), // Look one year into the future
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        model.addAttribute("user", authentication.getPrincipal());
        model.addAttribute("doctorName", doctor.getName());
        model.addAttribute("visits", scheduledVisits); // Pass the whole Page object

        return "doctor/dashboard";
    }
}
