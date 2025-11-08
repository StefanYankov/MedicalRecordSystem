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

    public AdminDashboardController(PatientService patientService, DoctorService doctorService, VisitService visitService, DiagnosisService diagnosisService, SickLeaveService sickLeaveService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.visitService = visitService;
        this.diagnosisService = diagnosisService;
        this.sickLeaveService = sickLeaveService;
    }

    @GetMapping
    public String adminLandingPage() {
        logger.info("GET /admin: Displaying admin landing page.");
        return "admin/landing";
    }

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
