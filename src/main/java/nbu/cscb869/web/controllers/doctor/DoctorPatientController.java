package nbu.cscb869.web.controllers.doctor;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.viewmodels.PatientHistoryViewModel;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller for doctors to browse and view patient information.
 */
@Controller
@RequestMapping("/doctor/patients")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorPatientController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorPatientController.class);
    private final PatientService patientService;
    private final VisitService visitService;
    private final DoctorService doctorService;
    private final ModelMapper modelMapper;

    public DoctorPatientController(final PatientService patientService, final VisitService visitService, final DoctorService doctorService, final ModelMapper modelMapper) {
        this.patientService = patientService;
        this.visitService = visitService;
        this.doctorService = doctorService;
        this.modelMapper = modelMapper;
    }

    /**
     * Displays a paginated and searchable list of all patients in the system.
     *
     * @param model     The Spring UI model.
     * @param pageable  Pagination information.
     * @param keyword   The search keyword (optional).
     * @return The logical name of the patient list view.
     */
    @GetMapping
    public String listPatients(final Model model,
                               @PageableDefault(size = 10, sort = "name") final Pageable pageable,
                               @RequestParam(required = false) final String keyword) {

        logger.info("GET /doctor/patients: Displaying patient list for doctor. Search keyword: '{}', Pageable: {}", keyword, pageable);

        final Page<PatientViewDTO> patientPage = patientService.findAll(pageable, keyword);

        model.addAttribute("patientPage", patientPage);
        model.addAttribute("keyword", keyword);

        return "doctor/patients/list";
    }

    /**
     * Displays the medical history for a specific patient.
     *
     * @param id    The ID of the patient.
     * @param model The Spring UI model.
     * @return The logical name of the patient medical history view.
     */
    @GetMapping("/{id}/history")
    public String showPatientHistory(@PathVariable final Long id, final Model model) {
        logger.info("GET /doctor/patients/{}/history: Displaying medical history for patient with ID {}", id, id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String keycloakId = authentication.getName(); // Use getName() for robustness
        DoctorViewDTO currentDoctor = doctorService.getByKeycloakId(keycloakId);
        model.addAttribute("loggedInDoctorId", currentDoctor.getId());

        final PatientViewDTO patient = patientService.getById(id);
        final List<VisitViewDTO> visits = visitService.getVisitsByPatient(id, 0, 100).getContent();

        PatientHistoryViewModel patientHistoryViewModel = new PatientHistoryViewModel();
        patientHistoryViewModel.setId(patient.getId());
        patientHistoryViewModel.setName(patient.getName());
        patientHistoryViewModel.setEgn(patient.getEgn());
        patientHistoryViewModel.setLastInsurancePaymentDate(patient.getLastInsurancePaymentDate());

        if (patient.getGeneralPractitionerId() != null) {
            try {
                DoctorViewDTO gpDoctor = doctorService.getById(patient.getGeneralPractitionerId());
                patientHistoryViewModel.setGeneralPractitioner(gpDoctor);
            } catch (EntityNotFoundException e) {
                logger.error("General practitioner with ID {} not found for patient {}.", patient.getGeneralPractitionerId(), patient.getId(), e);
                // Optionally handle this error, e.g., set a default or null GP
            }
        }

        patientHistoryViewModel.setVisits(visits);

        model.addAttribute("patient", patientHistoryViewModel);

        return "doctor/patients/history";
    }
}
