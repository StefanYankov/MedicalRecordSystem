package nbu.cscb869.web.controllers.admin;

import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/admin/specialties")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpecialtyController {
    private static final Logger logger = LoggerFactory.getLogger(AdminSpecialtyController.class);
    private final SpecialtyService specialtyService;

    public AdminSpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public String listSpecialties(Model model) throws ExecutionException, InterruptedException {
        logger.info("Admin request to list all specialties.");
        model.addAttribute("specialties", specialtyService.getAll(0, 100, "name", true).get().getContent());
        return "admin/specialties/list";
    }

    @GetMapping("/create")
    public String showCreateSpecialtyForm(Model model) {
        logger.info("Admin request to show create specialty form.");
        model.addAttribute("specialty", new SpecialtyCreateDTO());
        return "admin/specialties/form";
    }

    @PostMapping("/create")
    public String createSpecialty(@ModelAttribute SpecialtyCreateDTO specialty) {
        logger.info("Admin request to create a new specialty: {}", specialty.getName());
        specialtyService.create(specialty);
        return "redirect:/admin/specialties";
    }

    @GetMapping("/edit/{id}")
    public String showEditSpecialtyForm(@PathVariable Long id, Model model) {
        logger.info("Admin request to show edit form for specialty ID: {}", id);
        model.addAttribute("specialty", specialtyService.getById(id));
        return "admin/specialties/form";
    }

    @PostMapping("/edit/{id}")
    public String editSpecialty(@PathVariable Long id, @ModelAttribute SpecialtyUpdateDTO specialty) {
        specialty.setId(id);
        logger.info("Admin request to update specialty ID: {}", id);
        specialtyService.update(specialty);
        return "redirect:/admin/specialties";
    }

    @PostMapping("/delete/{id}")
    public String deleteSpecialty(@PathVariable Long id) {
        logger.info("Admin request to delete specialty ID: {}", id);
        specialtyService.delete(id);
        return "redirect:/admin/specialties";
    }
}
