package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.VisitService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
@SecurityRequirement(name = "bearerAuth")
public class VisitController {
    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Create a visit")
    @PostMapping
    public ResponseEntity<VisitViewDTO> create(@RequestBody VisitCreateDTO dto) {
        return ResponseEntity.ok(visitService.create(dto));
    }

    @Operation(summary = "Update a visit")
    @PutMapping
    public ResponseEntity<VisitViewDTO> update(@RequestBody VisitUpdateDTO dto) {
        return ResponseEntity.ok(visitService.update(dto));
    }

    @Operation(summary = "Delete a visit")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        visitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a visit by ID")
    @GetMapping("/{id}")
    public ResponseEntity<VisitViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(visitService.getById(id));
    }

    @Operation(summary = "Get all visits with pagination and filter")
    @GetMapping
    public ResponseEntity<Page<VisitViewDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "visitDate") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(visitService.getAll(page, size, orderBy, ascending, filter).join());
    }

    @Operation(summary = "Get visits by patient")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<VisitViewDTO>> findByPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(visitService.findByPatient(patientId, page, size).join());
    }

    @Operation(summary = "Get visits by diagnosis")
    @GetMapping("/diagnosis/{diagnosisId}")
    public ResponseEntity<Page<VisitViewDTO>> findByDiagnosis(
            @PathVariable Long diagnosisId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(visitService.findByDiagnosis(diagnosisId, page, size).join());
    }

    @Operation(summary = "Get visits by date range")
    @GetMapping("/range")
    public ResponseEntity<Page<VisitViewDTO>> findByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(visitService.findByDateRange(startDate, endDate, page, size).join());
    }

    @Operation(summary = "Get most frequent diagnoses")
    @GetMapping("/diagnoses/most-frequent")
    public ResponseEntity<List<DiagnosisVisitCountDTO>> findMostFrequentDiagnoses() {
        return ResponseEntity.ok(visitService.findMostFrequentDiagnoses());
    }

    @Operation(summary = "Get month with most sick leaves")
    @GetMapping("/sick-leaves/most-month")
    public ResponseEntity<Integer> findMonthWithMostSickLeaves() {
        return ResponseEntity.ok(visitService.findMonthWithMostSickLeaves());
    }
}