package nbu.cscb869.web.controllers.doctor;

import jakarta.validation.Valid;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor/profile")
public class DoctorProfileController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorProfileController.class);
    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;
    private final ModelMapper modelMapper;

    public DoctorProfileController(final DoctorService doctorService, final SpecialtyService specialtyService, final ModelMapper modelMapper) {
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public String showPendingApprovalPage() {
        return "doctor/pending-approval";
    }

    @GetMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public String showCompleteProfileForm(final Model model, @AuthenticationPrincipal final OidcUser principal) throws ExecutionException, InterruptedException {
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

        repopulateSpecialtiesForForm(model, Collections.emptySet());
        model.addAttribute("fullName", principal.getFullName());

        return "doctor/complete-profile";
    }

    @PostMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public String completeProfile(@Valid @ModelAttribute("doctor") final DoctorCreateDTO createDTO, final BindingResult bindingResult, final Model model, @AuthenticationPrincipal final OidcUser principal) throws ExecutionException, InterruptedException {
        final String keycloakId = principal.getSubject();
        logger.info("POST /doctor/profile/complete: Processing profile for user {}", keycloakId);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors for user {}: {}", keycloakId, bindingResult.getAllErrors());
            repopulateSpecialtiesForForm(model, createDTO.getSpecialties());
            model.addAttribute("fullName", principal.getFullName());
            return "doctor/complete-profile";
        }

        try {
            doctorService.getByUniqueIdNumber(createDTO.getUniqueIdNumber());
            bindingResult.rejectValue("uniqueIdNumber", "error.doctor", ExceptionMessages.formatDoctorUniqueIdExists(createDTO.getUniqueIdNumber()));
            logger.warn("Attempted to create a doctor with a duplicate UIN: {}", createDTO.getUniqueIdNumber());
            repopulateSpecialtiesForForm(model, createDTO.getSpecialties());
            model.addAttribute("fullName", principal.getFullName());
            return "doctor/complete-profile";
        } catch (EntityNotFoundException e) {
            // This is the happy path, UIN is unique
        }

        createDTO.setKeycloakId(keycloakId);
        doctorService.createDoctor(createDTO);

        logger.info("Successfully processed profile for user {}. Redirecting to pending approval page.", keycloakId);
        return "redirect:/doctor/profile/pending";
    }

    @GetMapping("/edit")
    @PreAuthorize("hasRole('DOCTOR')")
    public String showEditProfileForm(final Model model, @AuthenticationPrincipal final OidcUser principal) throws ExecutionException, InterruptedException {
        final String keycloakId = principal.getSubject();
        logger.info("GET /doctor/profile/edit: Preparing edit profile form for user {}", keycloakId);

        DoctorViewDTO doctorView = doctorService.getByKeycloakId(keycloakId);
        DoctorUpdateDTO doctorUpdate = modelMapper.map(doctorView, DoctorUpdateDTO.class);
        doctorUpdate.setSpecialties(doctorView.getSpecialties()); // Explicitly set specialties

        model.addAttribute("doctorUpdate", doctorUpdate);
        model.addAttribute("doctorView", doctorView);
        repopulateSpecialtiesForForm(model, doctorView.getSpecialties());

        return "doctor/edit-profile";
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('DOCTOR')")
    public String editProfile(@Valid @ModelAttribute("doctorUpdate") final DoctorUpdateDTO doctorUpdateDTO,
                              final BindingResult bindingResult,
                              final Model model,
                              @AuthenticationPrincipal final OidcUser principal,
                              @RequestParam(name = "imageFile", required = false) final MultipartFile imageFile,
                              final RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        final String keycloakId = principal.getSubject();
        logger.info("POST /doctor/profile/edit: Processing edit profile for user {}", keycloakId);

        DoctorViewDTO doctor = doctorService.getByKeycloakId(keycloakId);
        doctorUpdateDTO.setId(doctor.getId());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors for user {}: {}", keycloakId, bindingResult.getAllErrors());
            model.addAttribute("doctorView", doctor);
            repopulateSpecialtiesForForm(model, doctorUpdateDTO.getSpecialties());
            return "doctor/edit-profile";
        }

        try {
            doctorService.update(doctorUpdateDTO, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            logger.info("Successfully updated profile for user {}. Redirecting to edit profile page.", keycloakId);
            return "redirect:/doctor/profile/edit";
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", keycloakId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
            model.addAttribute("doctorView", doctor);
            repopulateSpecialtiesForForm(model, doctorUpdateDTO.getSpecialties());
            return "doctor/edit-profile";
        }
    }

    @PostMapping("/delete-image")
    @PreAuthorize("hasRole('DOCTOR')")
    public String deleteProfileImage(@AuthenticationPrincipal final OidcUser principal, final RedirectAttributes redirectAttributes) {
        final String keycloakId = principal.getSubject();
        logger.info("POST /doctor/profile/delete-image: Deleting profile image for user {}", keycloakId);
        try {
            DoctorViewDTO doctor = doctorService.getByKeycloakId(keycloakId);
            doctorService.deleteDoctorImage(doctor.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Profile image deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting image for user {}: {}", keycloakId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting image: " + e.getMessage());
        }
        return "redirect:/doctor/profile/edit";
    }

    private void repopulateSpecialtiesForForm(Model model, Set<String> assignedNames) throws ExecutionException, InterruptedException {
        List<SpecialtyViewDTO> allSpecialties = specialtyService.getAll(0, 100, "name", true).get().getContent();
        Set<String> assignedSpecialtyNames = assignedNames != null ? assignedNames : Collections.emptySet();

        List<SpecialtyViewDTO> assignedSpecialties = allSpecialties.stream()
                .filter(spec -> assignedSpecialtyNames.contains(spec.getName()))
                .collect(Collectors.toList());

        List<SpecialtyViewDTO> availableSpecialties = allSpecialties.stream()
                .filter(spec -> !assignedSpecialtyNames.contains(spec.getName()))
                .collect(Collectors.toList());

        model.addAttribute("availableSpecialties", availableSpecialties);
        model.addAttribute("assignedSpecialties", assignedSpecialties);
    }
}
