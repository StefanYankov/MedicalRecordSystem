package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sick-leaves")
@SecurityRequirement(name = "bearerAuth")
public class SickLeaveController {
    private final SickLeaveService sickLeaveService;

    public SickLeaveController(SickLeaveService sickLeaveService) {
        this.sickLeaveService = sickLeaveService;
    }

    @Operation(summary = "Create a sick leave")
    @PostMapping
    public ResponseEntity<SickLeaveViewDTO> create(@RequestBody SickLeaveCreateDTO dto) {
        return ResponseEntity.ok(sickLeaveService.create(dto));
    }

    @Operation(summary = "Update a sick leave")
    @PutMapping
    public ResponseEntity<SickLeaveViewDTO> update(@RequestBody SickLeaveUpdateDTO dto) {
        return ResponseEntity.ok(sickLeaveService.update(dto));
    }

    @Operation(summary = "Delete a sick leave")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sickLeaveService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a sick leave by ID")
    @GetMapping("/{id}")
    public ResponseEntity<SickLeaveViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sickLeaveService.getById(id));
    }

    @Operation(summary = "Get all sick leaves with pagination")
    @GetMapping
    public ResponseEntity<Page<SickLeaveViewDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String orderBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        return ResponseEntity.ok(sickLeaveService.getAll(page, size, orderBy, ascending).join());
    }
}