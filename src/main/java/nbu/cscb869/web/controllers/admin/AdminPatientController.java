package nbu.cscb869.web.controllers.admin;

import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.web.viewmodels.AdminPatientViewModel;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/patients")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPatientController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPatientController.class);
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final ModelMapper modelMapper;

    public AdminPatientController(PatientService patientService, DoctorService doctorService, ModelMapper modelMapper) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String listPatients(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/patients: Admin request to list all patients for page {} with size {}.", page, size);
        Page<PatientViewDTO> patientDtoPage = patientService.getAll(page, size, "name", true, null).get();

        List<AdminPatientViewModel> patientViewModels = patientDtoPage.getContent().stream()
                .map(dto -> {
                    AdminPatientViewModel viewModel = modelMapper.map(dto, AdminPatientViewModel.class);
                    if (dto.getGeneralPractitionerId() != null) {
                        try {
                            String gpName = doctorService.getById(dto.getGeneralPractitionerId()).getName();
                            viewModel.setGeneralPractitionerName(gpName);
                        } catch (Exception e) {
                            viewModel.setGeneralPractitionerName("N/A");
                        }
                    }
                    return viewModel;
                })
                .collect(Collectors.toList());

        Page<AdminPatientViewModel> viewModelPage = new PageImpl<>(patientViewModels, patientDtoPage.getPageable(), patientDtoPage.getTotalElements());

        model.addAttribute("patients", viewModelPage);
        return "admin/patients/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditPatientForm(@PathVariable("id") Long id, Model model) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/patients/edit/{}: Admin request to show edit form for patient ID: {}", id, id);
        PatientViewDTO patient = patientService.getById(id);
        model.addAttribute("patient", patient);
        model.addAttribute("doctors", doctorService.getAllAsync(0, 100, "name", true, null).get().getContent());
        return "admin/patients/edit";
    }

    @PostMapping("/edit/{id}")
    public String editPatient(@PathVariable("id") Long id, @ModelAttribute("patient") PatientUpdateDTO patientUpdateDTO, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/patients/edit/{}: Admin request to update patient ID: {}", id, id);
        try {
            patientUpdateDTO.setId(id);
            patientService.update(patientUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Patient updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating patient with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating patient: " + e.getMessage());
        }
        return "redirect:/admin/patients";
    }

    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        logger.info("GET /admin/patients/delete/{}: Admin request to show delete confirmation for patient ID: {}", id, id);
        model.addAttribute("patient", patientService.getById(id));
        return "admin/patients/delete-confirm";
    }

    @PostMapping("/delete/{id}")
    public String deletePatientConfirmed(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/patients/delete/{}: Admin request to confirm deletion of patient ID: {}", id, id);
        try {
            patientService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Patient deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting patient with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting patient: " + e.getMessage());
        }
        return "redirect:/admin/patients";
    }

    @PostMapping("/{id}/update-insurance")
    public String updateInsuranceStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/patients/{}/update-insurance: Admin request to update insurance for patient ID: {}", id, id);
        patientService.updateInsuranceStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Successfully updated insurance status for patient ID: " + id);
        return "redirect:/admin/patients/edit/" + id;
    }

    @PostMapping("/{id}/update-insurance-manual")
    public String updateInsuranceStatusManual(@PathVariable("id") Long id, @RequestParam("manualInsuranceDate") LocalDate manualInsuranceDate, RedirectAttributes redirectAttributes) {
        logger.info("POST /admin/patients/{}/update-insurance-manual: Admin request to manually update insurance for patient ID: {} to date: {}", id, id, manualInsuranceDate);
        PatientViewDTO patient = patientService.getById(id);
        PatientUpdateDTO updateDTO = modelMapper.map(patient, PatientUpdateDTO.class);
        updateDTO.setLastInsurancePaymentDate(manualInsuranceDate);
        patientService.update(updateDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Successfully updated insurance status for patient ID: " + id);
        return "redirect:/admin/patients/edit/" + id;
    }
}
