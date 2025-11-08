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

@Controller
@RequestMapping("/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDoctorController.class);
    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;
    private final ModelMapper modelMapper;

    public AdminDoctorController(DoctorService doctorService, SpecialtyService specialtyService, ModelMapper modelMapper) {
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String listDoctors(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/doctors: Admin request to list all doctors for page {} with size {}.", page, size);
        Page<DoctorViewDTO> doctorPage = doctorService.getAllAsync(page, size, "name", true, null).get();
        model.addAttribute("doctors", doctorPage);
        return "admin/doctors/list";
    }

    @GetMapping("/unapproved")
    public String listUnapprovedDoctors(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        logger.info("GET /admin/doctors/unapproved: Admin request to list unapproved doctors for page {} with size {}.", page, size);
        Page<DoctorViewDTO> doctorPage = doctorService.getUnapprovedDoctors(page, size);
        model.addAttribute("doctors", doctorPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "admin/doctors/unapproved-list";
    }

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

    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable Long id, Model model) {
        logger.info("GET /admin/doctors/delete/{}: Admin request to show delete confirmation for doctor ID: {}", id, id);
        model.addAttribute("doctor", doctorService.getById(id));
        return "admin/doctors/delete-confirm";
    }

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
