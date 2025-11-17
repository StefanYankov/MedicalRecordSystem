package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * RESTful API Controller for managing Diagnosis entities.
 * Provides endpoints for creating, retrieving, updating, and deleting diagnoses.
 * All endpoints are restricted to users with the 'ADMIN' role.
 */
@RestController
@RequestMapping("/api/diagnoses")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Diagnosis API", description = "Endpoints for managing medical diagnoses.")
@ApiStandardResponses
public class DiagnosisApiController {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosisApiController.class);
    private final DiagnosisService diagnosisService;

    public DiagnosisApiController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @Operation(summary = "Get all diagnoses", description = "Retrieves a paginated list of all diagnoses.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of all diagnoses.
     *
     * @param pageable Pagination information.
     * @return A ResponseEntity containing a Page of {@link DiagnosisViewDTO} objects.
     */
    @GetMapping
    public ResponseEntity<Page<DiagnosisViewDTO>> getAllDiagnoses(Pageable pageable) throws ExecutionException, InterruptedException {
        logger.info("API GET request for all diagnoses. Pageable: {}", pageable);
        
        Pageable pageRequest = pageable.isPaged() ? pageable : PageRequest.of(0, 10, pageable.getSort());

        Page<DiagnosisViewDTO> diagnoses = diagnosisService.getAll(pageRequest.getPageNumber(), pageRequest.getPageSize(), "name", true, null).get();
        return ResponseEntity.ok(diagnoses);
    }

    @Operation(summary = "Get a diagnosis by ID")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a single diagnosis by its ID.
     *
     * @param id The ID of the diagnosis to retrieve.
     * @return A ResponseEntity containing the {@link DiagnosisViewDTO}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DiagnosisViewDTO> getDiagnosisById(@Parameter(description = "The ID of the diagnosis to retrieve.") @PathVariable Long id) {
        logger.info("API GET request for diagnosis with ID: {}", id);
        return ResponseEntity.ok(diagnosisService.getById(id));
    }

    @Operation(summary = "Create a new diagnosis")
    @ApiResponse(responseCode = "201", description = OpenApiConstants.SUCCESS_CREATED)
    /**
     * Creates a new diagnosis.
     *
     * @param createDTO The DTO containing the data for the new diagnosis.
     * @return A ResponseEntity with status 201 (Created) and the created {@link DiagnosisViewDTO}.
     */
    @PostMapping
    public ResponseEntity<DiagnosisViewDTO> createDiagnosis(@RequestBody @Valid DiagnosisCreateDTO createDTO) {
        logger.info("API POST request to create a new diagnosis.");
        DiagnosisViewDTO createdDiagnosis = diagnosisService.create(createDTO);
        return ResponseEntity.created(URI.create("/api/diagnoses/" + createdDiagnosis.getId())).body(createdDiagnosis);
    }

    @Operation(summary = "Update an existing diagnosis")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Updates an existing diagnosis.
     *
     * @param id The ID of the diagnosis to update.
     * @param updateData The DTO containing the updated data.
     * @return A ResponseEntity containing the updated {@link DiagnosisViewDTO}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<DiagnosisViewDTO> updateDiagnosis(
            @Parameter(description = "The ID of the diagnosis to update.") @PathVariable Long id,
            @RequestBody @Valid DiagnosisCreateDTO updateData) {
        logger.info("API PATCH request to update diagnosis with ID: {}", id);
        DiagnosisUpdateDTO serviceDto = new DiagnosisUpdateDTO();
        serviceDto.setId(id);
        serviceDto.setName(updateData.getName());
        serviceDto.setDescription(updateData.getDescription());

        DiagnosisViewDTO updatedDiagnosis = diagnosisService.update(serviceDto);
        return ResponseEntity.ok(updatedDiagnosis);
    }

    @Operation(summary = "Delete a diagnosis")
    @ApiResponse(responseCode = "204", description = OpenApiConstants.SUCCESS_NO_CONTENT)
    /**
     * Deletes a diagnosis by its ID.
     *
     * @param id The ID of the diagnosis to delete.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiagnosis(@Parameter(description = "The ID of the diagnosis to delete.") @PathVariable Long id) {
        logger.info("API DELETE request for diagnosis with ID: {}", id);
        diagnosisService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
