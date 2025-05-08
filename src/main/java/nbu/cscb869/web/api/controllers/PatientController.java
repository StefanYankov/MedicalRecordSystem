package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.PatientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Create a patient")
    @PostMapping
    public ResponseEntity<PatientViewDTO> create(@RequestBody PatientCreateDTO dto) {
        return ResponseEntity.ok(patientService.create(dto));
    }

    @Operation(summary = "Update a patient")
    @PutMapping
    public ResponseEntity<PatientViewDTO> update(@RequestBody PatientUpdateDTO dto) {
        return ResponseEntity.ok(patientService.update(dto));
    }

    @Operation(summary = "Delete a patient")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a patient by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PatientViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @Operation(summary = "Get a patient by EGN")
    @GetMapping("/egn/{egn}")
    public ResponseEntity<PatientViewDTO> getByEgn(@PathVariable String egn) {
        return ResponseEntity.ok(patientService.getByEgn(egn));
    }

    @Operation(summary = "Get all patients with pagination and filter")
    @GetMapping
    public ResponseEntity<Page<PatientViewDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(patientService.getAll(page, size, orderBy, ascending, filter).join());
    }

    @Operation(summary = "Get patients by general practitioner")
    @GetMapping("/gp/{gpId}")
    public ResponseEntity<Page<PatientViewDTO>> getByGeneralPractitioner(
            @PathVariable Long gpId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(patientService.getByGeneralPractitioner(gpId, PageRequest.of(page, size)));
    }

    @Operation(summary = "Get patient count by general practitioner")
    @GetMapping("/gp/count")
    public ResponseEntity<List<DoctorPatientCountDTO>> getPatientCountByGeneralPractitioner() {
        return ResponseEntity.ok(patientService.getPatientCountByGeneralPractitioner());
    }
}