package nbu.cscb869.web.controllers.admin;

import jakarta.validation.Valid;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
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
@RequestMapping("/admin/sick-leaves")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSickLeaveController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSickLeaveController.class);
    private final SickLeaveService sickLeaveService;
    private final VisitService visitService;
    private final ModelMapper modelMapper;

    public AdminSickLeaveController(SickLeaveService sickLeaveService, VisitService visitService, ModelMapper modelMapper) {
        this.sickLeaveService = sickLeaveService;
        this.visitService = visitService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String listSickLeaves(Model model,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/sick-leaves: Admin request to list all sick leaves for page {} with size {}.", page, size);
        Page<SickLeaveViewDTO> sickLeavePage = sickLeaveService.getAll(page, size, "startDate", false).get();
        model.addAttribute("sickLeaves", sickLeavePage);
        return "admin/sick-leaves/list";
    }

    @GetMapping("/create")
    public String showCreateSickLeaveForm(Model model) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/sick-leaves/create: Admin request to show create form for sick leave.");
        if (!model.containsAttribute("sickLeave")) {
            model.addAttribute("sickLeave", new SickLeaveCreateDTO());
        }
        model.addAttribute("visits", visitService.getAll(0, 100, "visitDate", false, null).get().getContent());
        return "admin/sick-leaves/create";
    }

    @PostMapping("/create")
    public String createSickLeave(@Valid @ModelAttribute("sickLeave") SickLeaveCreateDTO sickLeaveCreateDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/sick-leaves/create: Admin request to create new sick leave.");

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors occurred while creating sick leave. Errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.sickLeave", bindingResult);
            redirectAttributes.addFlashAttribute("sickLeave", sickLeaveCreateDTO);
            return "redirect:/admin/sick-leaves/create";
        }

        try {
            sickLeaveService.create(sickLeaveCreateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Sick leave created successfully.");
        } catch (Exception e) {
            logger.error("Error creating sick leave: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating sick leave: " + e.getMessage());
            redirectAttributes.addFlashAttribute("sickLeave", sickLeaveCreateDTO);
            return "redirect:/admin/sick-leaves/create";
        }
        return "redirect:/admin/sick-leaves";
    }

    @GetMapping("/edit/{id}")
    public String showEditSickLeaveForm(@PathVariable("id") Long id, Model model) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/sick-leaves/edit/{}: Admin request to show edit form for sick leave ID: {}", id, id);
        if (!model.containsAttribute("sickLeave")) {
            SickLeaveViewDTO sickLeaveViewDTO = sickLeaveService.getById(id);
            model.addAttribute("sickLeave", modelMapper.map(sickLeaveViewDTO, SickLeaveUpdateDTO.class));
        }
        model.addAttribute("visits", visitService.getAll(0, 100, "visitDate", false, null).get().getContent());
        return "admin/sick-leaves/edit";
    }

    @PostMapping("/edit/{id}")
    public String editSickLeave(@PathVariable("id") Long id, @Valid @ModelAttribute("sickLeave") SickLeaveUpdateDTO sickLeaveUpdateDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/sick-leaves/edit/{}: Admin request to update sick leave ID: {}", id, id);

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors occurred while updating sick leave ID: {}. Errors: {}", id, bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.sickLeave", bindingResult);
            redirectAttributes.addFlashAttribute("sickLeave", sickLeaveUpdateDTO);
            return "redirect:/admin/sick-leaves/edit/" + id;
        }

        try {
            sickLeaveUpdateDTO.setId(id);
            sickLeaveService.update(sickLeaveUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Sick leave updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating sick leave with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating sick leave: " + e.getMessage());
            redirectAttributes.addFlashAttribute("sickLeave", sickLeaveUpdateDTO);
            return "redirect:/admin/sick-leaves/edit/" + id;
        }
        return "redirect:/admin/sick-leaves";
    }

    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        logger.info("GET /admin/sick-leaves/delete/{}: Admin request to show delete confirmation for sick leave ID: {}", id, id);
        model.addAttribute("sickLeave", sickLeaveService.getById(id));
        return "admin/sick-leaves/delete-confirm";
    }

    @PostMapping("/delete/{id}")
    public String deleteSickLeaveConfirmed(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/sick-leaves/delete/{}: Admin request to confirm deletion of sick leave ID: {}", id, id);
        try {
            sickLeaveService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Sick leave deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting sick leave with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting sick leave: " + e.getMessage());
        }
        return "redirect:/admin/sick-leaves";
    }
}
