package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import nbu.cscb869.services.services.contracts.TreatmentService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/treatments")
@SecurityRequirement(name = "bearerAuth")
public class TreatmentController {
    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @Operation(summary = "Create a treatment")
    @PostMapping
    public ResponseEntity<TreatmentViewDTO> create(@RequestBody TreatmentCreateDTO dto) {
        return ResponseEntity.ok(treatmentService.create(dto));
    }

    @Operation(summary = "Update a treatment")
    @PutMapping
    public ResponseEntity<TreatmentViewDTO> update(@RequestBody TreatmentUpdateDTO dto) {
        return ResponseEntity.ok(treatmentService.update(dto));
    }

    @Operation(summary = "Delete a treatment")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        treatmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a treatment by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TreatmentViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(treatmentService.getById(id));
    }

    @Operation(summary = "Get all treatments with pagination")
    @GetMapping
    public ResponseEntity<Page<TreatmentViewDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "description") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        return ResponseEntity.ok(treatmentService.getAll(page, size, orderBy, ascending).join());
    }
}