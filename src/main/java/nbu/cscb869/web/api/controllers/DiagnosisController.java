package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagnoses")
@SecurityRequirement(name = "bearerAuth")
public class DiagnosisController {
    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @Operation(summary = "Create a diagnosis")
    @PostMapping
    public ResponseEntity<DiagnosisViewDTO> create(@RequestBody DiagnosisCreateDTO dto) {
        return ResponseEntity.ok(diagnosisService.create(dto));
    }

    @Operation(summary = "Update a diagnosis")
    @PutMapping
    public ResponseEntity<DiagnosisViewDTO> update(@RequestBody DiagnosisUpdateDTO dto) {
        return ResponseEntity.ok(diagnosisService.update(dto));
    }

    @Operation(summary = "Delete a diagnosis")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        diagnosisService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a diagnosis by ID")
    @GetMapping("/{id}")
    public ResponseEntity<DiagnosisViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(diagnosisService.getById(id));
    }

    @Operation(summary = "Get all diagnoses with pagination and filter")
    @GetMapping
    public ResponseEntity<Page<DiagnosisViewDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(diagnosisService.getAll(page, size, orderBy, ascending, filter).join());
    }
}