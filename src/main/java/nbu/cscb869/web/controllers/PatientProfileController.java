package nbu.cscb869.web.controllers;

import nbu.cscb869.config.WebConstants;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.viewmodels.MedicalHistoryVisitViewModel;
import nbu.cscb869.web.viewmodels.SickLeaveHistoryViewModel;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import nbu.cscb869.data.models.Doctor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
public class PatientProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PatientProfileController.class);
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final VisitService visitService;
    private final ModelMapper modelMapper;

    public PatientProfileController(PatientService patientService, DoctorService doctorService, VisitService visitService, ModelMapper modelMapper) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.visitService = visitService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/complete")
    public String showCompleteProfileForm(Model model, @AuthenticationPrincipal OidcUser principal) {
        logger.info("GET /profile/complete: Preparing profile form for user {}", principal.getName());
        try {
            PatientViewDTO existingPatient = patientService.getByKeycloakId(principal.getSubject());
            logger.debug("Found existing patient record for user {}. Pre-populating form for update.", principal.getName());
            model.addAttribute("patient", modelMapper.map(existingPatient, PatientUpdateDTO.class));
        } catch (EntityNotFoundException e) {
            logger.debug("No existing patient record for user {}. Preparing form for new patient.", principal.getName());
            model.addAttribute("patient", new PatientCreateDTO());
        }

        Specification<Doctor> isGpSpec = (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("isGeneralPractitioner"));
        model.addAttribute("generalPractitioners", doctorService.findByCriteria(isGpSpec, 0, 100, "name", true).getContent());
        return "complete-profile";
    }

    @PostMapping("/complete")
    public String completeProfile(@ModelAttribute("patient") PatientUpdateDTO patientDTO, @AuthenticationPrincipal OidcUser principal) {
        logger.info("POST /profile/complete: Processing profile for user {}", principal.getName());
        try {
            PatientViewDTO existingPatient = patientService.getByKeycloakId(principal.getSubject());
            logger.debug("User {} is an existing patient. Updating record.", principal.getName());
            patientDTO.setId(existingPatient.getId());
            patientService.update(patientDTO);
        } catch (EntityNotFoundException e) {
            logger.debug("User {} is a new patient. Creating record.", principal.getName());
            PatientCreateDTO createDTO = modelMapper.map(patientDTO, PatientCreateDTO.class);
            createDTO.setKeycloakId(principal.getSubject());
            createDTO.setName(principal.getFullName());
            patientService.create(createDTO);
        }
        logger.info("Successfully processed profile for user {}. Redirecting to dashboard.", principal.getName());
        return "redirect:/profile/dashboard";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientDashboard(Model model, @AuthenticationPrincipal OidcUser principal) {
        logger.info("GET /profile/dashboard: Displaying dashboard for user {}.", principal.getName());
        PatientViewDTO patient = patientService.getByKeycloakId(principal.getSubject());
        Page<VisitViewDTO> upcomingVisits = visitService.getVisitsByDoctorAndStatusAndDateRange(patient.getGeneralPractitionerId(), VisitStatus.SCHEDULED, LocalDate.now(), LocalDate.now().plusYears(1), 0, 5);

        model.addAttribute("patient", patient);
        model.addAttribute("upcomingVisits", upcomingVisits.getContent());
        return "patient-dashboard";
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT')")
    public String medicalHistory(Model model, @AuthenticationPrincipal OidcUser principal,
                                 @RequestParam(defaultValue = WebConstants.DEFAULT_PAGE_NUMBER) int page,
                                 @RequestParam(defaultValue = WebConstants.DEFAULT_PAGE_SIZE) int size) {
        logger.info("GET /profile/history: Displaying medical history for user {}, page {}, size {}", principal.getName(), page, size);
        PatientViewDTO patient = patientService.getByKeycloakId(principal.getSubject());
        Page<VisitViewDTO> serviceDtoPage = visitService.getVisitsByPatient(patient.getId(), page, size);

        // Map from the rich service DTO to the lean ViewModel
        Page<MedicalHistoryVisitViewModel> viewModelPage = serviceDtoPage.map(dto -> {
            MedicalHistoryVisitViewModel vm = new MedicalHistoryVisitViewModel();
            vm.setVisitId(dto.getId());
            vm.setVisitDate(dto.getVisitDate());
            vm.setStatus(dto.getStatus());
            if (dto.getDoctor() != null) {
                vm.setDoctorName(dto.getDoctor().getName());
            }
            if (dto.getDiagnosis() != null) {
                vm.setDiagnosisName(dto.getDiagnosis().getName());
            }
            return vm;
        });

        model.addAttribute("visitPage", viewModelPage);
        return "medical-history";
    }

    @GetMapping("/sick-leaves")
    @PreAuthorize("hasRole('PATIENT')")
    public String sickLeaveHistory(Model model, @AuthenticationPrincipal OidcUser principal) {
        logger.info("GET /profile/sick-leaves: Displaying sick leave history for user {}.", principal.getName());
        PatientViewDTO patient = patientService.getByKeycloakId(principal.getSubject());

        List<VisitViewDTO> allVisits = new ArrayList<>();
        Page<VisitViewDTO> visitPage;
        int currentPage = 0;
        do {
            visitPage = visitService.getVisitsByPatient(patient.getId(), currentPage, WebConstants.MAX_PAGE_SIZE);
            allVisits.addAll(visitPage.getContent());
            currentPage++;
        } while (!visitPage.isLast());

        List<SickLeaveHistoryViewModel> sickLeaves = allVisits.stream()
                .filter(visit -> visit.getSickLeave() != null)
                .map(visit -> {
                    SickLeaveHistoryViewModel vm = new SickLeaveHistoryViewModel();
                    vm.setStartDate(visit.getSickLeave().getStartDate());
                    vm.setDurationDays(visit.getSickLeave().getDurationDays());
                    if (visit.getDoctor() != null) {
                        vm.setDoctorName(visit.getDoctor().getName());
                    }
                    if (visit.getDiagnosis() != null) {
                        vm.setDiagnosisName(visit.getDiagnosis().getName());
                    }
                    return vm;
                })
                .collect(Collectors.toList());

        model.addAttribute("sickLeaves", sickLeaves);
        return "profile/sick-leaves";
    }
}
