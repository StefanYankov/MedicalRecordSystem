package nbu.cscb869.web.controllers.admin;

import jakarta.validation.Valid;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.VisitUpdateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/admin/visits")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVisitController {

    private static final Logger logger = LoggerFactory.getLogger(AdminVisitController.class);
    private final VisitService visitService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DiagnosisService diagnosisService;
    private final ModelMapper modelMapper;

    public AdminVisitController(VisitService visitService, PatientService patientService, DoctorService doctorService, DiagnosisService diagnosisService, ModelMapper modelMapper) {
        this.visitService = visitService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.diagnosisService = diagnosisService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String listVisits(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String filter) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/visits: Admin request to list all visits for page {} with size {} and filter '{}'.", page, size, filter);
        Page<VisitViewDTO> visitPage = visitService.getAll(page, size, "visitDate", false, filter).get();
        model.addAttribute("visits", visitPage);
        model.addAttribute("filter", filter);
        return "admin/visits/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditVisitForm(@PathVariable("id") Long id, Model model) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/visits/edit/{}: Admin request to show edit form for visit ID: {}", id, id);
        if (!model.containsAttribute("visit")) {
            VisitViewDTO visitViewDTO = visitService.getById(id);
            model.addAttribute("visit", modelMapper.map(visitViewDTO, VisitUpdateDTO.class));
        }

        model.addAttribute("patients", patientService.getAll(0, 100, "name", true, null).get().getContent());
        model.addAttribute("doctors", doctorService.getAllAsync(0, 100, "name", true, null).get().getContent());
        model.addAttribute("diagnoses", diagnosisService.getAll(0, 100, "name", true, null).get().getContent());
        model.addAttribute("statuses", VisitStatus.values());
        return "admin/visits/edit";
    }

    @PostMapping("/edit/{id}")
    public String editVisit(@PathVariable("id") Long id, @Valid @ModelAttribute("visit") VisitUpdateDTO visitUpdateDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) throws ExecutionException, InterruptedException {
        logger.info("POST /admin/visits/edit/{}: Admin request to update visit ID: {}", id, id);

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors occurred while updating visit ID: {}. Errors: {}", id, bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.visit", bindingResult);
            redirectAttributes.addFlashAttribute("visit", visitUpdateDTO);
            return "redirect:/admin/visits/edit/" + id;
        }

        try {
            visitUpdateDTO.setId(id);
            visitService.update(visitUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Visit updated successfully.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred while updating visit ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating visit: " + e.getMessage());
        }
        return "redirect:/admin/visits";
    }

    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        logger.info("GET /admin/visits/delete/{}: Admin request to show delete confirmation for visit ID: {}", id, id);
        model.addAttribute("visit", visitService.getById(id));
        return "admin/visits/delete-confirm";
    }

    @PostMapping("/delete/{id}")
    public String deleteVisitConfirmed(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/visits/delete/{}: Admin request to confirm deletion of visit ID: {}", id, id);
        try {
            visitService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Visit deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting visit with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting visit: " + e.getMessage());
        }
        return "redirect:/admin/visits";
    }
}
