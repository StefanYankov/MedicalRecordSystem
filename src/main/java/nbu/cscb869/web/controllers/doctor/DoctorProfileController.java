package nbu.cscb869.web.controllers.doctor;

import jakarta.validation.Valid;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor/profile")
public class DoctorProfileController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorProfileController.class);
    private final DoctorService doctorService;
    private final SpecialtyRepository specialtyRepository;
    private final ModelMapper modelMapper;

    public DoctorProfileController(final DoctorService doctorService, final SpecialtyRepository specialtyRepository, final ModelMapper modelMapper) {
        this.doctorService = doctorService;
        this.specialtyRepository = specialtyRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public String showPendingApprovalPage() {
        return "doctor/pending-approval";
    }

    @GetMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public String showCompleteProfileForm(final Model model, @AuthenticationPrincipal final OidcUser principal) {
        final String keycloakId = principal.getSubject();
        logger.info("GET /doctor/profile/complete: Preparing profile form for user {}", keycloakId);

        if (!model.containsAttribute("doctor")) {
            try {
                final DoctorViewDTO existingDoctor = doctorService.getByKeycloakId(keycloakId);
                logger.debug("Found existing doctor record for user {}. Pre-populating form for update.", keycloakId);
                model.addAttribute("doctor", modelMapper.map(existingDoctor, DoctorUpdateDTO.class));
            } catch (EntityNotFoundException e) {
                logger.debug("No existing doctor record for user {}. Preparing form for new doctor.", keycloakId);
                DoctorCreateDTO newDoctor = new DoctorCreateDTO();
                newDoctor.setName(principal.getFullName()); // Pre-populate the name
                model.addAttribute("doctor", newDoctor);
            }
        }

        populateSpecialtyLists(model);
        model.addAttribute("fullName", principal.getFullName());

        return "doctor/complete-profile";
    }

    @PostMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public String completeProfile(@Valid @ModelAttribute("doctor") final DoctorCreateDTO createDTO, final BindingResult bindingResult, final Model model, @AuthenticationPrincipal final OidcUser principal) {
        final String keycloakId = principal.getSubject();
        logger.info("POST /doctor/profile/complete: Processing profile for user {}", keycloakId);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors for user {}: {}", keycloakId, bindingResult.getAllErrors());
            populateSpecialtyLists(model);
            model.addAttribute("fullName", principal.getFullName());
            return "doctor/complete-profile";
        }

        try {
            doctorService.getByUniqueIdNumber(createDTO.getUniqueIdNumber());
            bindingResult.rejectValue("uniqueIdNumber", "error.doctor", ExceptionMessages.formatDoctorUniqueIdExists(createDTO.getUniqueIdNumber()));
            logger.warn("Attempted to create a doctor with a duplicate UIN: {}", createDTO.getUniqueIdNumber());
            populateSpecialtyLists(model);
            model.addAttribute("fullName", principal.getFullName());
            return "doctor/complete-profile";
        } catch (EntityNotFoundException e) {
            // This is the expected path - the UIN is unique
        }

        createDTO.setKeycloakId(keycloakId);
        doctorService.createDoctor(createDTO);

        logger.info("Successfully processed profile for user {}. Redirecting to pending approval page.", keycloakId);
        return "redirect:/doctor/profile/pending";
    }

    private void populateSpecialtyLists(Model model) {
        List<Specialty> allSpecialties = specialtyRepository.findAll();
        Set<String> assignedSpecialtyNames = Collections.emptySet();

        if (model.containsAttribute("doctor")) {
            Object doctorAttr = model.getAttribute("doctor");
            if (doctorAttr instanceof DoctorUpdateDTO) {
                assignedSpecialtyNames = ((DoctorUpdateDTO) doctorAttr).getSpecialties();
            } else if (doctorAttr instanceof DoctorCreateDTO) {
                assignedSpecialtyNames = ((DoctorCreateDTO) doctorAttr).getSpecialties();
            }
        }

        final Set<String> finalAssignedSpecialtyNames = assignedSpecialtyNames != null ? assignedSpecialtyNames : Collections.emptySet();

        List<Specialty> availableSpecialties = allSpecialties.stream()
                .filter(spec -> !finalAssignedSpecialtyNames.contains(spec.getName()))
                .collect(Collectors.toList());

        List<Specialty> assignedSpecialties = allSpecialties.stream()
                .filter(spec -> finalAssignedSpecialtyNames.contains(spec.getName()))
                .collect(Collectors.toList());

        model.addAttribute("availableSpecialties", availableSpecialties);
        model.addAttribute("assignedSpecialties", assignedSpecialties);
    }
}
