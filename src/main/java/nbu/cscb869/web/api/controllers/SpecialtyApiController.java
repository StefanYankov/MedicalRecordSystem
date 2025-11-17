package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * RESTful API Controller for managing Specialty entities.
 * Provides endpoints for creating, retrieving, updating, and deleting specialties.
 * All endpoints are restricted to users with the 'ADMIN' role.
 */
@RestController
@RequestMapping("/api/specialties")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Specialty API", description = "Endpoints for managing medical specialties.")
@ApiStandardResponses
public class SpecialtyApiController {
    private static final Logger logger = LoggerFactory.getLogger(SpecialtyApiController.class);
    private final SpecialtyService specialtyService;

    public SpecialtyApiController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @Operation(summary = "Get all specialties", description = "Retrieves a paginated list of all specialties.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of all specialties.
     *
     * @param pageable Pagination information.
     * @return A ResponseEntity containing a Page of {@link SpecialtyViewDTO} objects.
     */
    @GetMapping
    public ResponseEntity<Page<SpecialtyViewDTO>> getAllSpecialties(@Parameter(description = "Pagination information.") Pageable pageable) throws ExecutionException, InterruptedException {
        logger.info("API GET request for all specialties. Pageable: {}", pageable);
        Page<SpecialtyViewDTO> specialties = specialtyService.getAll(pageable.getPageNumber(), pageable.getPageSize(), "name", true).get();
        return ResponseEntity.ok(specialties);
    }

    @Operation(summary = "Get a specialty by ID")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a single specialty by its ID.
     *
     * @param id The ID of the specialty to retrieve.
     * @return A ResponseEntity containing the {@link SpecialtyViewDTO}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpecialtyViewDTO> getSpecialtyById(@Parameter(description = "The ID of the specialty to retrieve.") @PathVariable Long id) {
        logger.info("API GET request for specialty with ID: {}", id);
        return ResponseEntity.ok(specialtyService.getById(id));
    }

    @Operation(summary = "Create a new specialty")
    @ApiResponse(responseCode = "201", description = OpenApiConstants.SUCCESS_CREATED)
    /**
     * Creates a new specialty.
     *
     * @param createDTO The DTO containing the data for the new specialty.
     * @return A ResponseEntity with status 201 (Created) and the created {@link SpecialtyViewDTO}.
     */
    @PostMapping
    public ResponseEntity<SpecialtyViewDTO> createSpecialty(@RequestBody @Valid SpecialtyCreateDTO createDTO) {
        logger.info("API POST request to create a new specialty.");
        SpecialtyViewDTO createdSpecialty = specialtyService.create(createDTO);
        return ResponseEntity.created(URI.create("/api/specialties/" + createdSpecialty.getId())).body(createdSpecialty);
    }

    @Operation(summary = "Update an existing specialty")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Updates an existing specialty.
     *
     * @param id The ID of the specialty to update.
     * @param updateDTO The DTO containing the updated data.
     * @return A ResponseEntity containing the updated {@link SpecialtyViewDTO}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SpecialtyViewDTO> updateSpecialty(
            @Parameter(description = "The ID of the specialty to update.") @PathVariable Long id,
            @RequestBody @Valid SpecialtyUpdateDTO updateDTO) {
        logger.info("API PATCH request to update specialty with ID: {}", id);
        updateDTO.setId(id);
        SpecialtyViewDTO updatedSpecialty = specialtyService.update(updateDTO);
        return ResponseEntity.ok(updatedSpecialty);
    }

    @Operation(summary = "Delete a specialty")
    @ApiResponse(responseCode = "204", description = OpenApiConstants.SUCCESS_NO_CONTENT)
    /**
     * Deletes a specialty by its ID.
     *
     * @param id The ID of the specialty to delete.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecialty(@Parameter(description = "The ID of the specialty to delete.") @PathVariable Long id) {
        logger.info("API DELETE request for specialty with ID: {}", id);
        specialtyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
