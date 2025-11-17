package nbu.cscb869.web.controllers.admin;

import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import nbu.cscb869.web.viewmodels.DoctorEditViewModel;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Controller for administrative management of doctor entities.
 * Provides functionality for listing, approving, editing, and deleting doctors.
 * All endpoints are secured and require the user to have the 'ADMIN' role.
 */
@Controller
@RequestMapping("/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDoctorController.class);
    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;
    private final ModelMapper modelMapper;

    /**
     * Constructs the controller with necessary services and a ModelMapper.
     *
     * @param doctorService    Service for doctor-related operations.
     * @param specialtyService Service for specialty-related data.
     * @param modelMapper      The ModelMapper instance for DTO-entity conversion.
     */
    public AdminDoctorController(DoctorService doctorService, SpecialtyService specialtyService, ModelMapper modelMapper) {
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
        this.modelMapper = modelMapper;
    }

    /**
     * Displays a paginated list of all doctors in the system.
     *
     * @param model The Spring UI model.
     * @param page  The requested page number.
     * @param size  The number of items per page.
     * @return The logical name of the doctor list view.
     * @throws ExecutionException   If an error occurs during asynchronous data fetching.
     * @throws InterruptedException If a thread is interrupted during asynchronous data fetching.
     */
    @GetMapping
    public String listDoctors(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/doctors: Admin request to list all doctors for page {} with size {}.", page, size);
        Page<DoctorViewDTO> doctorPage = doctorService.getAllAsync(page, size, "name", true, null).get();
        model.addAttribute("doctors", doctorPage);
        return "admin/doctors/list";
    }

    /**
     * Displays a paginated list of doctors who are awaiting approval.
     *
     * @param model The Spring UI model.
     * @param page  The requested page number.
     * @param size  The number of items per page.
     * @return The logical name of the unapproved doctors list view.
     */
    @GetMapping("/unapproved")
    public String listUnapprovedDoctors(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        logger.info("GET /admin/doctors/unapproved: Admin request to list unapproved doctors for page {} with size {}.", page, size);
        Page<DoctorViewDTO> doctorPage = doctorService.getUnapprovedDoctors(page, size);
        model.addAttribute("doctors", doctorPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "admin/doctors/unapproved-list";
    }

    /**
     * Processes the approval of a doctor.
     *
     * @param id                 The ID of the doctor to approve.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string to the unapproved doctors list.
     */
    @PostMapping("/{id}/approve")
    public String approveDoctor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/doctors/{}/approve: Admin request to approve doctor ID: {}", id, id);
        try {
            doctorService.approveDoctor(id);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor approved successfully.");
        } catch (Exception e) {
            logger.error("Error approving doctor with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving doctor: " + e.getMessage());
        }
        return "redirect:/admin/doctors/unapproved";
    }

    /**
     * Displays the form for editing an existing doctor's details, including their specialties.
     *
     * @param id    The ID of the doctor to edit.
     * @param model The Spring UI model.
     * @return The logical name of the doctor edit view.
     * @throws ExecutionException   If an error occurs during asynchronous data fetching.
     * @throws InterruptedException If a thread is interrupted during asynchronous data fetching.
     */
    @GetMapping("/edit/{id}")
    public String showEditDoctorForm(@PathVariable Long id, Model model) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/doctors/edit/{}: Admin request to show edit form for doctor ID: {}", id, id);
        DoctorViewDTO doctorViewDTO = doctorService.getById(id);
        DoctorEditViewModel viewModel = modelMapper.map(doctorViewDTO, DoctorEditViewModel.class);

        List<SpecialtyViewDTO> allSpecialties = specialtyService.getAll(0, 100, "name", true).get().getContent();
        Set<String> assignedSpecialtyNames = doctorViewDTO.getSpecialties();

        List<SpecialtyViewDTO> assignedSpecialties = allSpecialties.stream()
                .filter(spec -> assignedSpecialtyNames.contains(spec.getName()))
                .collect(Collectors.toList());

        List<SpecialtyViewDTO> availableSpecialties = allSpecialties.stream()
                .filter(spec -> !assignedSpecialtyNames.contains(spec.getName()))
                .collect(Collectors.toList());

        model.addAttribute("doctor", viewModel);
        model.addAttribute("assignedSpecialties", assignedSpecialties);
        model.addAttribute("availableSpecialties", availableSpecialties);

        return "admin/doctors/edit";
    }

    /**
     * Processes the submission of the doctor edit form.
     *
     * @param id                 The ID of the doctor being edited.
     * @param doctor             The DTO containing the updated form data.
     * @param imageFile          The new profile image file, if one was uploaded.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string to the main doctors list.
     */
    @PostMapping("/edit/{id}")
    public String editDoctor(@PathVariable Long id, @ModelAttribute("doctor") DoctorUpdateDTO doctor, @RequestParam("imageFile") MultipartFile imageFile, RedirectAttributes redirectAttributes) {
        doctor.setId(id);
        logger.info("POST /admin/doctors/edit/{}: Admin request to update doctor ID: {}", id, id);
        try {
            doctorService.update(doctor, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating doctor with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating doctor: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    /**
     * Displays a confirmation page before deleting a doctor.
     *
     * @param id    The ID of the doctor to be deleted.
     * @param model The Spring UI model.
     * @return The logical name of the delete confirmation view.
     */
    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable Long id, Model model) {
        logger.info("GET /admin/doctors/delete/{}: Admin request to show delete confirmation for doctor ID: {}", id, id);
        model.addAttribute("doctor", doctorService.getById(id));
        return "admin/doctors/delete-confirm";
    }

    /**
     * Processes the confirmed deletion of a doctor.
     *
     * @param id                 The ID of the doctor to delete.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string to the main doctors list.
     */
    @PostMapping("/delete/{id}")
    public String deleteDoctorConfirmed(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/doctors/delete/{}: Admin request to confirm deletion of doctor ID: {}", id, id);
        try {
            doctorService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting doctor with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting doctor: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    /**
     * Processes the deletion of a doctor's profile image.
     *
     * @param id                 The ID of the doctor whose image is to be deleted.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string back to the doctor edit page.
     */
    @PostMapping("/{id}/delete-image")
    public String deleteDoctorImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/doctors/{}/delete-image: Admin request to delete image for doctor ID: {}", id, id);
        try {
            doctorService.deleteDoctorImage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor image deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting image for doctor with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting image: " + e.getMessage());
        }
        return "redirect:/admin/doctors/edit/" + id;
    }
}
