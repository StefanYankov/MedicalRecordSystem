package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.DoctorService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {
    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Operation(summary = "Create a doctor")
    @PostMapping
    public ResponseEntity<DoctorViewDTO> create(@RequestPart DoctorCreateDTO dto, @RequestPart(required = false) MultipartFile image) {
        return ResponseEntity.ok(doctorService.create(dto, image));
    }

    @Operation(summary = "Update a doctor")
    @PutMapping
    public ResponseEntity<DoctorViewDTO> update(@RequestPart DoctorUpdateDTO dto, @RequestPart(required = false) MultipartFile image) {
        return ResponseEntity.ok(doctorService.update(dto, image));
    }

    @Operation(summary = "Delete a doctor")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a doctor by ID")
    @GetMapping("/{id}")
    public ResponseEntity<DoctorViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @Operation(summary = "Get all doctors with pagination and filter")
    @GetMapping
    public ResponseEntity<Page<DoctorViewDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(doctorService.getAll(page, size, orderBy, ascending, filter).join());
    }

    @Operation(summary = "Get doctors by criteria")
    @GetMapping("/search")
    public ResponseEntity<Page<DoctorViewDTO>> findByCriteria(
            @RequestParam Map<String, Object> conditions,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        return ResponseEntity.ok(doctorService.findByCriteria(conditions, page, size, orderBy, ascending));
    }

    @Operation(summary = "Get patients by general practitioner")
    @GetMapping("/{id}/patients")
    public ResponseEntity<Page<PatientViewDTO>> getPatients(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getPatientsByGeneralPractitioner(id));
    }

    @Operation(summary = "Get visit count for a doctor")
    @GetMapping("/{id}/visits/count")
    public ResponseEntity<DoctorVisitCountDTO> getVisitCount(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getVisitCount(id));
    }

    @Operation(summary = "Get visits by period for a doctor")
    @GetMapping("/{id}/visits")
    public ResponseEntity<Page<VisitViewDTO>> getVisitsByPeriod(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(doctorService.getVisitsByPeriod(id, startDate, endDate).join());
    }

    @Operation(summary = "Get doctors with most sick leaves")
    @GetMapping("/sick-leaves/most")
    public ResponseEntity<List<DoctorSickLeaveCountDTO>> getDoctorsWithMostSickLeaves() {
        return ResponseEntity.ok(doctorService.getDoctorsWithMostSickLeaves());
    }
}