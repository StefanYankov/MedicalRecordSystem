package nbu.cscb869.web.controllers.admin;

import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.viewmodels.AdminDashboardViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.ExecutionException;

/**
 * Controller for handling the main administrative dashboard and landing page.
 * This controller aggregates data from various services to provide a high-level overview
 * of the system's state for administrators. All endpoints within this controller
 * are secured and require the user to have the 'ADMIN' role.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final VisitService visitService;
    private final DiagnosisService diagnosisService;
    private final SickLeaveService sickLeaveService;

    /**
     * Constructs the controller with all necessary services for data aggregation.
     *
     * @param patientService   Service for patient-related data.
     * @param doctorService    Service for doctor-related data.
     * @param visitService     Service for visit-related data.
     * @param diagnosisService Service for diagnosis-related data.
     * @param sickLeaveService Service for sick leave-related data.
     */
    public AdminDashboardController(PatientService patientService, DoctorService doctorService, VisitService visitService, DiagnosisService diagnosisService, SickLeaveService sickLeaveService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.visitService = visitService;
        this.diagnosisService = diagnosisService;
        this.sickLeaveService = sickLeaveService;
    }

    /**
     * Displays the main administrative landing page.
     * This serves as the entry point to the admin section.
     *
     * @return The logical name of the admin landing view.
     */
    @GetMapping
    public String adminLandingPage() {
        logger.info("GET /admin: Displaying admin landing page.");
        return "admin/landing";
    }

    /**
     * Gathers system-wide metrics and displays the main administrative dashboard.
     * This method fetches counts for total patients, doctors, visits, etc., and also
     * retrieves a list of recent visits and unapproved doctors to be displayed.
     *
     * @param model The Spring UI model to which the dashboard data will be added.
     * @return The logical name of the admin dashboard view.
     * @throws ExecutionException   If an error occurs during asynchronous data fetching.
     * @throws InterruptedException If a thread is interrupted during asynchronous data fetching.
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/dashboard: Displaying admin dashboard with metrics.");
        AdminDashboardViewModel viewModel = new AdminDashboardViewModel();
        viewModel.setTotalPatients(patientService.getAll(0, 1, "id", false, null).get().getTotalElements());
        viewModel.setTotalDoctors(doctorService.getAllAsync(0, 1, "id", false, null).get().getTotalElements());
        viewModel.setTotalVisits(visitService.getAll(0, 1, "id", false, null).get().getTotalElements());
        viewModel.setTotalDiagnoses(diagnosisService.getTotalDiagnosesCount());
        viewModel.setTotalSickLeaves(sickLeaveService.getTotalSickLeavesCount());
        viewModel.setRecentVisits(visitService.getAll(0, 5, "visitDate", false, null).get().getContent());
        viewModel.setUnapprovedDoctorsCount(doctorService.getUnapprovedDoctors(0, 1).getTotalElements());

        model.addAttribute("dashboard", viewModel);
        return "admin/dashboard";
    }
}
