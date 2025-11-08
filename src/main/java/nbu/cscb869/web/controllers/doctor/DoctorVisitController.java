package nbu.cscb869.web.controllers.doctor;

import jakarta.validation.Valid;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.VisitDocumentationDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import nbu.cscb869.services.services.contracts.TreatmentService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * Controller for doctors to document and manage patient visits.
 */
@Controller
@RequestMapping("/doctor/visits")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorVisitController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorVisitController.class);
    private final VisitService visitService;
    private final DoctorService doctorService;
    private final DiagnosisService diagnosisService;
    private final TreatmentService treatmentService;
    private final SickLeaveService sickLeaveService;

    public DoctorVisitController(final VisitService visitService, final DoctorService doctorService,
                                 final DiagnosisService diagnosisService, final TreatmentService treatmentService,
                                 final SickLeaveService sickLeaveService) {
        this.visitService = visitService;
        this.doctorService = doctorService;
        this.diagnosisService = diagnosisService;
        this.treatmentService = treatmentService;
        this.sickLeaveService = sickLeaveService;
    }

    /**
     * Displays the form for documenting a specific visit.
     * Ensures that only the doctor who conducted the visit can access this page.
     *
     * @param visitId   The ID of the visit to document.
     * @param model     The Spring UI model.
     * @param principal The authenticated OIDC user principal.
     * @return The logical name of the visit documentation view.
     * @throws EntityNotFoundException if the visit is not found.
     * @throws AccessDeniedException if the logged-in doctor did not conduct this visit.
     */
    @GetMapping("/{visitId}/document")
    public String showDocumentVisitForm(@PathVariable final Long visitId, final Model model,
                                        @AuthenticationPrincipal final OidcUser principal) {
        logger.info("GET /doctor/visits/{}/document: Preparing documentation form for visit ID {}", visitId, visitId);

        final VisitViewDTO visit = visitService.getById(visitId);

        // Security check: Ensure the logged-in doctor is the one who conducted this visit
        final Long loggedInDoctorId = doctorService.getByKeycloakId(principal.getSubject()).getId();
        if (!visit.getDoctor().getId().equals(loggedInDoctorId)) {
            logger.warn("Access denied: Doctor {} attempted to document visit {} not conducted by them.", loggedInDoctorId, visitId);
            throw new AccessDeniedException("You are not authorized to document this visit.");
        }

        model.addAttribute("visit", visit);
        // Pre-populate DTOs for the form, if they are not already in the model (e.g., from a failed submission)
        if (!model.containsAttribute("visitDocumentationDTO")) {
            VisitDocumentationDTO visitDocumentationDTO = new VisitDocumentationDTO();
            visitDocumentationDTO.setVisitId(visitId);
            visitDocumentationDTO.setNotes(visit.getNotes());
            if (visit.getDiagnosis() != null) {
                visitDocumentationDTO.setDiagnosisId(visit.getDiagnosis().getId());
            }
            // TODO: Map existing treatment and sick leave to DTOs if they exist
            model.addAttribute("visitDocumentationDTO", visitDocumentationDTO);
        }

        try {
            // Fetch all diagnoses for selection, assuming a large enough page size to get all.
            model.addAttribute("allDiagnoses", diagnosisService.getAll(0, 1000, "name", true, null).get().getContent());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching all diagnoses: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            model.addAttribute("errorMessage", "Could not load diagnoses. Please try again.");
            return "error"; // Or handle more gracefully
        }

        return "doctor/visits/document";
    }

    /**
     * Processes the submission of the visit documentation form.
     *
     * @param visitId   The ID of the visit being documented.
     * @param visitDocumentationDTO The DTO containing the documentation data.
     * @param bindingResult Validation results.
     * @param principal The authenticated OIDC user principal.
     * @param redirectAttributes Attributes for redirect.
     * @return A redirect to the patient's history page or back to the form on error.
     */
    @PostMapping("/{visitId}/document")
    public String documentVisit(@PathVariable final Long visitId,
                                @Valid @ModelAttribute("visitDocumentationDTO") final VisitDocumentationDTO visitDocumentationDTO,
                                final BindingResult bindingResult,
                                final Model model,
                                @AuthenticationPrincipal final OidcUser principal,
                                final RedirectAttributes redirectAttributes) {

        logger.info("POST /doctor/visits/{}/document: Processing documentation for visit ID {}", visitId, visitId);

        // Security check (re-verify on POST)
        final VisitViewDTO visit = visitService.getById(visitId);
        final Long loggedInDoctorId = doctorService.getByKeycloakId(principal.getSubject()).getId();
        if (!visit.getDoctor().getId().equals(loggedInDoctorId)) {
            logger.warn("Access denied: Doctor {} attempted to document visit {} not conducted by them.", loggedInDoctorId, visitId);
            throw new AccessDeniedException("You are not authorized to document this visit.");
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during visit documentation for visit ID {}: {}", visitId, bindingResult.getAllErrors());
            model.addAttribute("visit", visit);
            try {
                model.addAttribute("allDiagnoses", diagnosisService.getAll(0, 1000, "name", true, null).get().getContent());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error fetching all diagnoses during validation error: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
                model.addAttribute("errorMessage", "Could not load diagnoses. Please try again.");
                return "error";
            }
            return "doctor/visits/document";
        }

        try {
            visitService.documentVisit(visitId, visitDocumentationDTO);
            logger.info("Successfully documented visit ID {}. Redirecting to patient history.", visitId);
            redirectAttributes.addFlashAttribute("successMessage", "Visit documented successfully!");
            return "redirect:/doctor/patients/" + visit.getPatient().getId() + "/history";
        } catch (Exception e) {
            logger.error("Error documenting visit ID {}: {}", visitId, e.getMessage(), e);
            bindingResult.reject("documentation.failed", "An unexpected error occurred during documentation. Please try again.");
            model.addAttribute("visit", visit);
            try {
                model.addAttribute("allDiagnoses", diagnosisService.getAll(0, 1000, "name", true, null).get().getContent());
            } catch (InterruptedException | ExecutionException eInner) {
                logger.error("Error fetching all diagnoses during documentation error: {}", eInner.getMessage(), eInner);
                Thread.currentThread().interrupt();
                model.addAttribute("errorMessage", "Could not load diagnoses. Please try again.");
                return "error";
            }
            return "doctor/visits/document";
        }
    }
}
