package nbu.cscb869.web.controllers.admin;

import jakarta.validation.Valid;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
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

/**
 * Controller for administrative management of medical diagnoses.
 * Provides full CRUD (Create, Read, Update, Delete) functionality for {@link nbu.cscb869.data.models.Diagnosis} entities.
 * All endpoints are secured and require the user to have the 'ADMIN' role.
 */
@Controller
@RequestMapping("/admin/diagnoses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiagnosisController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDiagnosisController.class);
    private final DiagnosisService diagnosisService;
    private final ModelMapper modelMapper;

    /**
     * Constructs the controller with the necessary services.
     *
     * @param diagnosisService The service for diagnosis-related operations.
     * @param modelMapper      The ModelMapper instance for DTO-entity conversion.
     */
    public AdminDiagnosisController(DiagnosisService diagnosisService, ModelMapper modelMapper) {
        this.diagnosisService = diagnosisService;
        this.modelMapper = modelMapper;
    }

    /**
     * Displays a paginated and filterable list of all diagnoses.
     *
     * @param model  The Spring UI model.
     * @param page   The requested page number.
     * @param size   The number of items per page.
     * @param filter An optional string to filter diagnoses by name.
     * @return The logical name of the diagnosis list view.
     * @throws ExecutionException   If an error occurs during asynchronous data fetching.
     * @throws InterruptedException If a thread is interrupted during asynchronous data fetching.
     */
    @GetMapping
    public String listDiagnoses(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String filter) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/diagnoses: Admin request to list all diagnoses for page {} with size {} and filter '{}'.", page, size, filter);
        Page<DiagnosisViewDTO> diagnosisPage = diagnosisService.getAll(page, size, "name", true, filter).get();
        model.addAttribute("diagnoses", diagnosisPage);
        model.addAttribute("filter", filter);
        return "admin/diagnoses/list";
    }

    /**
     * Displays the form for creating a new diagnosis.
     *
     * @param model The Spring UI model.
     * @return The logical name of the diagnosis creation view.
     */
    @GetMapping("/create")
    public String showCreateDiagnosisForm(Model model) {
        logger.info("GET /admin/diagnoses/create: Admin request to show create form for diagnosis.");
        if (!model.containsAttribute("diagnosis")) {
            logger.debug("Adding new DiagnosisCreateDTO to model for create form.");
            model.addAttribute("diagnosis", new DiagnosisCreateDTO());
        }
        return "admin/diagnoses/create";
    }

    /**
     * Processes the submission of the new diagnosis form.
     * On success, redirects to the diagnosis list. On validation error, returns to the form with errors.
     *
     * @param diagnosisCreateDTO The DTO containing the form data.
     * @param bindingResult      The result of the validation.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string to the appropriate view.
     */
    @PostMapping("/create")
    public String createDiagnosis(@Valid @ModelAttribute("diagnosis") DiagnosisCreateDTO diagnosisCreateDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/diagnoses/create: Admin request to create new diagnosis.");

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors occurred while creating diagnosis. Errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.diagnosis", bindingResult);
            redirectAttributes.addFlashAttribute("diagnosis", diagnosisCreateDTO);
            return "redirect:/admin/diagnoses/create";
        }

        try {
            diagnosisService.create(diagnosisCreateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Diagnosis created successfully.");
        } catch (Exception e) {
            logger.error("Error creating diagnosis: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating diagnosis: " + e.getMessage());
            redirectAttributes.addFlashAttribute("diagnosis", diagnosisCreateDTO);
            return "redirect:/admin/diagnoses/create";
        }
        return "redirect:/admin/diagnoses";
    }

    /**
     * Displays the form for editing an existing diagnosis.
     *
     * @param id    The ID of the diagnosis to edit.
     * @param model The Spring UI model.
     * @return The logical name of the diagnosis edit view.
     */
    @GetMapping("/edit/{id}")
    public String showEditDiagnosisForm(@PathVariable("id") Long id, Model model) {
        logger.info("GET /admin/diagnoses/edit/{}: Admin request to show edit form for diagnosis ID: {}", id, id);
        if (!model.containsAttribute("diagnosis")) {
            logger.debug("Fetching diagnosis ID {} for edit form.", id);
            DiagnosisViewDTO diagnosisViewDTO = diagnosisService.getById(id);
            model.addAttribute("diagnosis", modelMapper.map(diagnosisViewDTO, DiagnosisUpdateDTO.class));
        }
        return "admin/diagnoses/edit";
    }

    /**
     * Processes the submission of the diagnosis edit form.
     *
     * @param id                 The ID of the diagnosis being edited.
     * @param diagnosisUpdateDTO The DTO containing the updated form data.
     * @param bindingResult      The result of the validation.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string to the appropriate view.
     */
    @PostMapping("/edit/{id}")
    public String editDiagnosis(@PathVariable("id") Long id, @Valid @ModelAttribute("diagnosis") DiagnosisUpdateDTO diagnosisUpdateDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/diagnoses/edit/{}: Admin request to update diagnosis ID: {}", id, id);

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors occurred while updating diagnosis ID: {}. Errors: {}", id, bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.diagnosis", bindingResult);
            redirectAttributes.addFlashAttribute("diagnosis", diagnosisUpdateDTO);
            return "redirect:/admin/diagnoses/edit/" + id;
        }

        try {
            diagnosisUpdateDTO.setId(id);
            diagnosisService.update(diagnosisUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Diagnosis updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating diagnosis with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating diagnosis: " + e.getMessage());
            redirectAttributes.addFlashAttribute("diagnosis", diagnosisUpdateDTO);
            return "redirect:/admin/diagnoses/edit/" + id;
        }
        return "redirect:/admin/diagnoses";
    }

    /**
     * Displays a confirmation page before deleting a diagnosis.
     *
     * @param id    The ID of the diagnosis to be deleted.
     * @param model The Spring UI model.
     * @return The logical name of the delete confirmation view.
     */
    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        logger.info("GET /admin/diagnoses/delete/{}: Admin request to show delete confirmation for diagnosis ID: {}", id, id);
        model.addAttribute("diagnosis", diagnosisService.getById(id));
        return "admin/diagnoses/delete-confirm";
    }

    /**
     * Processes the confirmed deletion of a diagnosis.
     *
     * @param id                 The ID of the diagnosis to delete.
     * @param redirectAttributes Attributes for passing messages across redirects.
     * @return A redirect string to the diagnosis list view.
     */
    @PostMapping("/delete/{id}")
    public String deleteDiagnosisConfirmed(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/diagnoses/delete/{}: Admin request to confirm deletion of diagnosis ID: {}", id, id);
        try {
            diagnosisService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Diagnosis deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting diagnosis with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting diagnosis: " + e.getMessage());
        }
        return "redirect:/admin/diagnoses";
    }
}
