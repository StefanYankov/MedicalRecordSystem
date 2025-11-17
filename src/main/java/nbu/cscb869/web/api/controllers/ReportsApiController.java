package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.MonthSickLeaveCountDTO;
import nbu.cscb869.services.data.dtos.DoctorPatientCountReportDTO;
import nbu.cscb869.services.data.dtos.DoctorVisitCountReportDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RESTful API Controller for administrative reports.
 * All endpoints are restricted to users with the 'ADMIN' role.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Reports API", description = "Endpoints for generating administrative reports.")
@ApiStandardResponses
public class ReportsApiController {
    private static final Logger logger = LoggerFactory.getLogger(ReportsApiController.class);
    private final DoctorService doctorService;
    private final VisitService visitService;

    public ReportsApiController(DoctorService doctorService, VisitService visitService) {
        this.doctorService = doctorService;
        this.visitService = visitService;
    }

    @Operation(summary = "Get patients by diagnosis", description = "Retrieves a paginated list of patients who have been diagnosed with a specific condition.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of patients for a specific diagnosis.
     *
     * @param diagnosisId The ID of the diagnosis.
     * @param pageable    Pagination information.
     * @return A ResponseEntity containing a Page of {@link PatientViewDTO} objects.
     */
    @GetMapping("/patients-by-diagnosis")
    public ResponseEntity<Page<PatientViewDTO>> getPatientsByDiagnosis(
            @Parameter(description = "The ID of the diagnosis to filter by.") @RequestParam Long diagnosisId,
            @Parameter(description = "Pagination information.") Pageable pageable) {
        logger.info("API GET request for patients by diagnosis ID: {}", diagnosisId);
        Page<VisitViewDTO> visits = visitService.getVisitsByDiagnosis(diagnosisId, pageable.getPageNumber(), pageable.getPageSize());
        List<PatientViewDTO> patients = visits.getContent().stream()
                .map(VisitViewDTO::getPatient)
                .distinct()
                .collect(Collectors.toList());
        Page<PatientViewDTO> patientPage = new PageImpl<>(patients, pageable, visits.getTotalElements());
        return ResponseEntity.ok(patientPage);
    }

    @Operation(summary = "Get most frequent diagnoses", description = "Retrieves a list of the most frequent diagnoses based on visit counts.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a list of the most frequent diagnoses.
     *
     * @return A ResponseEntity containing a list of {@link DiagnosisVisitCountDTO}.
     */
    @GetMapping("/most-frequent-diagnoses")
    public ResponseEntity<List<DiagnosisVisitCountDTO>> getMostFrequentDiagnoses() {
        logger.info("API GET request for most frequent diagnoses report.");
        return ResponseEntity.ok(visitService.getMostFrequentDiagnoses());
    }

    @Operation(summary = "Get GP patient counts", description = "Retrieves the number of patients registered to each General Practitioner.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves the number of patients registered to each General Practitioner.
     *
     * @return A ResponseEntity containing a list of {@link DoctorPatientCountReportDTO}.
     */
    @GetMapping("/gp-patient-counts")
    public ResponseEntity<List<DoctorPatientCountReportDTO>> getGpPatientCounts() {
        logger.info("API GET request for GP patient counts report.");
        return ResponseEntity.ok(doctorService.getPatientCountReport());
    }

    @Operation(summary = "Get doctor visit counts", description = "Retrieves the total number of visits for each doctor.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves the number of visits for each doctor.
     *
     * @return A ResponseEntity containing a list of {@link DoctorVisitCountReportDTO}.
     */
    @GetMapping("/doctor-visit-counts")
    public ResponseEntity<List<DoctorVisitCountReportDTO>> getDoctorVisitCounts() {
        logger.info("API GET request for doctor visit counts report.");
        return ResponseEntity.ok(visitService.getVisitCountByDoctor());
    }

    @Operation(summary = "Get doctors with most sick leaves", description = "Retrieves a list of doctors who have issued the most sick leaves.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a list of doctors who have issued the most sick leaves.
     *
     * @return A ResponseEntity containing a list of {@link DoctorSickLeaveCountDTO}.
     */
    @GetMapping("/doctors-with-most-sick-leaves")
    public ResponseEntity<List<DoctorSickLeaveCountDTO>> getDoctorsWithMostSickLeaves() {
        logger.info("API GET request for doctors with most sick leaves report.");
        return ResponseEntity.ok(doctorService.getDoctorsWithMostSickLeaves());
    }

    @Operation(summary = "Get most frequent sick leave month", description = "Retrieves the month(s) with the highest number of issued sick leaves.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves the month(s) with the highest number of issued sick leaves.
     *
     * @return A ResponseEntity containing a list of {@link MonthSickLeaveCountDTO}.
     */
    @GetMapping("/most-frequent-sick-leave-month")
    public ResponseEntity<List<MonthSickLeaveCountDTO>> getMostFrequentSickLeaveMonth() {
        logger.info("API GET request for most frequent sick leave month report.");
        return ResponseEntity.ok(visitService.getMostFrequentSickLeaveMonth());
    }

    @Operation(summary = "Get visits by date range", description = "Retrieves a paginated list of all visits within a given date range.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of all visits within a given date range.
     *
     * @param startDate The start date of the range (format YYYY-MM-DD).
     * @param endDate   The end date of the range (format YYYY-MM-DD).
     * @param pageable  Pagination information.
     * @return A ResponseEntity containing a Page of {@link VisitViewDTO} objects.
     */
    @GetMapping("/visits-by-date")
    public ResponseEntity<Page<VisitViewDTO>> getVisitsByDateRange(
            @Parameter(description = "The start date of the range (format YYYY-MM-DD).") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "The end date of the range (format YYYY-MM-DD).") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Pagination information.") Pageable pageable) {
        logger.info("API GET request for visits by date range: {} to {}", startDate, endDate);
        return ResponseEntity.ok(visitService.getVisitsByDateRange(startDate, endDate, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @Operation(summary = "Get doctor visits by date range", description = "Retrieves a paginated list of visits for a specific doctor within a given date range.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of visits for a specific doctor within a given date range.
     *
     * @param doctorId  The ID of the doctor.
     * @param startDate The start date of the range (format YYYY-MM-DD).
     * @param endDate   The end date of the range (format YYYY-MM-DD).
     * @param pageable  Pagination information.
     * @return A ResponseEntity containing a Page of {@link VisitViewDTO} objects.
     */
    @GetMapping("/doctor-visits-by-date")
    public ResponseEntity<Page<VisitViewDTO>> getDoctorVisitsByDateRange(
            @Parameter(description = "The ID of the doctor.") @RequestParam Long doctorId,
            @Parameter(description = "The start date of the range (format YYYY-MM-DD).") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "The end date of the range (format YYYY-MM-DD).") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Pagination information.") Pageable pageable) {
        logger.info("API GET request for visits by doctor ID {} and date range: {} to {}", doctorId, startDate, endDate);
        return ResponseEntity.ok(visitService.getVisitsByDoctorAndDateRange(doctorId, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize()));
    }
}
