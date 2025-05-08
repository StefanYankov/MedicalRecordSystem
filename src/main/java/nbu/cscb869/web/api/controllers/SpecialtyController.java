package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/specialties")
@SecurityRequirement(name = "bearerAuth")
public class SpecialtyController {
    private final SpecialtyRepository specialtyRepository;

    public SpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Operation(summary = "Create a specialty")
    @PostMapping
    public ResponseEntity<Specialty> create(@RequestBody Specialty specialty) {
        return ResponseEntity.ok(specialtyRepository.save(specialty));
    }

    @Operation(summary = "Update a specialty")
    @PutMapping
    public ResponseEntity<Specialty> update(@RequestBody Specialty specialty) {
        return ResponseEntity.ok(specialtyRepository.save(specialty));
    }

    @Operation(summary = "Delete a specialty")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specialtyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a specialty by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Specialty> getById(@PathVariable Long id) {
        return ResponseEntity.ok(specialtyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specialty not found")));
    }

    @Operation(summary = "Get all specialties with pagination")
    @GetMapping
    public ResponseEntity<Page<Specialty>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(specialtyRepository.findAllActive(PageRequest.of(page, size)));
    }
}