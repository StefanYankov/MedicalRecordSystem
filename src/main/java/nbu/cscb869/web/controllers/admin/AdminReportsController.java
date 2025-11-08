package nbu.cscb869.web.controllers.admin;

import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportsController {

    private static final Logger logger = LoggerFactory.getLogger(AdminReportsController.class);
    private final VisitService visitService;
    private final DiagnosisService diagnosisService;
    private final DoctorService doctorService;

    public AdminReportsController(VisitService visitService, DiagnosisService diagnosisService, DoctorService doctorService) {
        this.visitService = visitService;
        this.diagnosisService = diagnosisService;
        this.doctorService = doctorService;
    }

    @GetMapping
    public String reportsIndex() {
        logger.info("GET /admin/reports: Displaying reports index.");
        return "admin/reports/index";
    }

    @GetMapping("/patients-by-diagnosis")
    public String getPatientsByDiagnosis(Model model,
                                         @RequestParam(required = false) Long diagnosisId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/reports/patients-by-diagnosis: Request for diagnosis ID: {}, page: {}, size: {}", diagnosisId, page, size);
        model.addAttribute("diagnoses", diagnosisService.getAll(0, 100, "name", true, null).get().getContent());

        if (diagnosisId != null) {
            model.addAttribute("visits", visitService.getVisitsByDiagnosis(diagnosisId, page, size));
            model.addAttribute("selectedDiagnosisId", diagnosisId);
        }

        return "admin/reports/patients-by-diagnosis";
    }

    @GetMapping("/most-frequent-diagnoses")
    public String getMostFrequentDiagnoses(Model model) {
        logger.info("GET /admin/reports/most-frequent-diagnoses: Request for most frequent diagnoses report.");
        model.addAttribute("diagnoses", visitService.getMostFrequentDiagnoses());
        return "admin/reports/most-frequent-diagnoses";
    }

    @GetMapping("/patients-by-gp")
    public String getPatientsByGp(Model model,
                                  @RequestParam(required = false) Long gpId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/reports/patients-by-gp: Request for GP ID: {}, page: {}, size: {}", gpId, page, size);
        model.addAttribute("doctors", doctorService.getAllAsync(0, 100, "name", true, null).get().getContent());

        if (gpId != null) {
            model.addAttribute("patients", doctorService.getPatientsByGeneralPractitioner(gpId, page, size));
            model.addAttribute("selectedGpId", gpId);
        }

        return "admin/reports/patients-by-gp";
    }

    @GetMapping("/patient-count-by-gp")
    public String getPatientCountByGp(Model model) {
        logger.info("GET /admin/reports/patient-count-by-gp: Request for patient count by GP report.");
        model.addAttribute("gpCounts", doctorService.getPatientCountByGeneralPractitioner());
        return "admin/reports/patient-count-by-gp";
    }

    @GetMapping("/visit-count-by-doctor")
    public String getVisitCountByDoctor(Model model) {
        logger.info("GET /admin/reports/visit-count-by-doctor: Request for visit count by doctor report.");
        model.addAttribute("visitCounts", doctorService.getVisitCount());
        return "admin/reports/visit-count-by-doctor";
    }

    @GetMapping("/visits-by-period")
    public String getVisitsByPeriod(Model model,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        logger.info("GET /admin/reports/visits-by-period: Request for start: {}, end: {}, page: {}, size: {}", startDate, endDate, page, size);
        if (startDate != null && endDate != null) {
            model.addAttribute("visits", visitService.getVisitsByDateRange(startDate, endDate, page, size));
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        }

        return "admin/reports/visits-by-period";
    }

    @GetMapping("/visits-by-doctor-and-period")
    public String getVisitsByDoctorAndPeriod(Model model,
                                             @RequestParam(required = false) Long doctorId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) throws ExecutionException, InterruptedException {
        logger.info("GET /admin/reports/visits-by-doctor-and-period: Request for doctor ID: {}, start: {}, end: {}, page: {}, size: {}", doctorId, startDate, endDate, page, size);
        model.addAttribute("doctors", doctorService.getAllAsync(0, 100, "name", true, null).get().getContent());

        if (doctorId != null && startDate != null && endDate != null) {
            model.addAttribute("visits", doctorService.getVisitsByPeriod(doctorId, startDate, endDate, page, size));
            model.addAttribute("selectedDoctorId", doctorId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        }

        return "admin/reports/visits-by-doctor-and-period";
    }

    @GetMapping("/most-frequent-sick-leave-month")
    public String getMostFrequentSickLeaveMonth(Model model) {
        logger.info("GET /admin/reports/most-frequent-sick-leave-month: Request for most frequent sick leave month report.");
        model.addAttribute("sickLeaveMonths", visitService.getMostFrequentSickLeaveMonth());
        return "admin/reports/most-frequent-sick-leave-month";
    }

    @GetMapping("/doctors-with-most-sick-leaves")
    public String getDoctorsWithMostSickLeaves(Model model) {
        logger.info("GET /admin/reports/doctors-with-most-sick-leaves: Request for doctors with most sick leaves report.");
        model.addAttribute("doctorsWithMostSickLeaves", doctorService.getDoctorsWithMostSickLeaves());
        return "admin/reports/doctors-with-most-sick-leaves";
    }
}
