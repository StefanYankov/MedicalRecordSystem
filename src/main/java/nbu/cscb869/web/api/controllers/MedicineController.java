package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.repositories.MedicineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medicines")
@SecurityRequirement(name = "bearerAuth")
public class MedicineController {
    private final MedicineRepository medicineRepository;

    public MedicineController(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    @Operation(summary = "Create a medicine")
    @PostMapping
    public ResponseEntity<Medicine> create(@RequestBody Medicine medicine) {
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    @Operation(summary = "Update a medicine")
    @PutMapping
    public ResponseEntity<Medicine> update(@RequestBody Medicine medicine) {
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    @Operation(summary = "Delete a medicine")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        medicineRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a medicine by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getById(@PathVariable Long id) {
        return ResponseEntity.ok(medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found")));
    }

    @Operation(summary = "Get all medicines with pagination")
    @GetMapping
    public ResponseEntity<Page<Medicine>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(medicineRepository.findAllActive(PageRequest.of(page, size)));
    }
}