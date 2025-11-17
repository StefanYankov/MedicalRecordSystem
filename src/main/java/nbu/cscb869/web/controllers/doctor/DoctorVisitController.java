package nbu.cscb869.web.controllers.doctor;

import jakarta.validation.Valid;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.config.WebConstants;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
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
    private final ModelMapper modelMapper;

    public DoctorVisitController(final VisitService visitService, final DoctorService doctorService,
                                 final DiagnosisService diagnosisService, final ModelMapper modelMapper) {
        this.visitService = visitService;
        this.doctorService = doctorService;
        this.diagnosisService = diagnosisService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/{visitId}/document")
    public String showDocumentVisitForm(@PathVariable final Long visitId, final Model model,
                                        @AuthenticationPrincipal final OidcUser principal) {
        logger.info("GET /doctor/visits/{}/document: Preparing documentation form for visit ID {}", visitId, visitId);

        try {
            final VisitViewDTO visit = visitService.getById(visitId);
            logger.debug("Successfully fetched visit with ID: {}", visitId);

            final Long loggedInDoctorId = doctorService.getByKeycloakId(principal.getSubject()).getId();
            if (!visit.getDoctor().getId().equals(loggedInDoctorId)) {
                logger.warn("Access denied: Doctor {} attempted to document visit {} not conducted by them.", loggedInDoctorId, visitId);
                throw new AccessDeniedException("You are not authorized to document this visit.");
            }

            model.addAttribute("visit", visit);
            model.addAttribute("statuses", VisitStatus.values());

            if (!model.containsAttribute("visitDocumentationDTO")) {
                VisitDocumentationDTO visitDocumentationDTO = new VisitDocumentationDTO();
                visitDocumentationDTO.setVisitId(visitId);
                visitDocumentationDTO.setNotes(visit.getNotes());
                visitDocumentationDTO.setStatus(visit.getStatus().name());

                if (visit.getDiagnosis() != null) {
                    visitDocumentationDTO.setDiagnosisId(visit.getDiagnosis().getId());
                }
                if (visit.getTreatment() != null) {
                    visitDocumentationDTO.setTreatment(modelMapper.map(visit.getTreatment(), TreatmentUpdateDTO.class));
                }
                if (visit.getSickLeave() != null) {
                    visitDocumentationDTO.setSickLeave(modelMapper.map(visit.getSickLeave(), SickLeaveUpdateDTO.class));
                }
                model.addAttribute("visitDocumentationDTO", visitDocumentationDTO);
                logger.debug("Populated new VisitDocumentationDTO for the form.");
            }

            List<DiagnosisViewDTO> allDiagnoses = new ArrayList<>();
            Page<DiagnosisViewDTO> diagnosisPage;
            int currentPage = 0;
            do {
                diagnosisPage = diagnosisService.getAll(currentPage, WebConstants.MAX_PAGE_SIZE, "name", true, null).get();
                allDiagnoses.addAll(diagnosisPage.getContent());
                currentPage++;
            } while (!diagnosisPage.isLast());
            model.addAttribute("allDiagnoses", allDiagnoses);
            logger.debug("Fetched {} diagnoses for the dropdown.", allDiagnoses.size());

            return "doctor/visits/document";

        } catch (EntityNotFoundException e) {
            logger.error("Visit not found with ID: {}", visitId, e);
            return "error/404";
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching all diagnoses: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            model.addAttribute("errorMessage", "Could not load diagnoses. Please try again.");
            return "error";
        }
    }

    @PostMapping("/{visitId}/document")
    public String documentVisit(@PathVariable final Long visitId,
                                @Valid @ModelAttribute("visitDocumentationDTO") final VisitDocumentationDTO visitDocumentationDTO,
                                final BindingResult bindingResult,
                                final Model model,
                                @AuthenticationPrincipal final OidcUser principal,
                                final RedirectAttributes redirectAttributes) {

        logger.info("POST /doctor/visits/{}/document: Processing documentation for visit ID {}", visitId, visitId);
        logger.debug("Received DTO: {}", visitDocumentationDTO);

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
                List<DiagnosisViewDTO> allDiagnoses = new ArrayList<>();
                Page<DiagnosisViewDTO> diagnosisPage;
                int currentPage = 0;
                do {
                    diagnosisPage = diagnosisService.getAll(currentPage, WebConstants.MAX_PAGE_SIZE, "name", true, null).get();
                    allDiagnoses.addAll(diagnosisPage.getContent());
                    currentPage++;
                } while (!diagnosisPage.isLast());
                model.addAttribute("allDiagnoses", allDiagnoses);
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
                List<DiagnosisViewDTO> allDiagnoses = new ArrayList<>();
                Page<DiagnosisViewDTO> diagnosisPage;
                int currentPage = 0;
                do {
                    diagnosisPage = diagnosisService.getAll(currentPage, WebConstants.MAX_PAGE_SIZE, "name", true, null).get();
                    allDiagnoses.addAll(diagnosisPage.getContent());
                    currentPage++;
                } while (!diagnosisPage.isLast());
                model.addAttribute("allDiagnoses", allDiagnoses);
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
