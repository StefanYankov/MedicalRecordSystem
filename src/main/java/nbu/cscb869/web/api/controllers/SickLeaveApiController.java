package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
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
 * RESTful API Controller for managing Sick Leave entities.
 * Provides endpoints for creating, retrieving, updating, and deleting sick leaves.
 * All endpoints are restricted to users with the 'ADMIN' role.
 */
@RestController
@RequestMapping("/api/sick-leaves")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Sick Leave API", description = "Endpoints for managing sick leave records.")
@ApiStandardResponses
public class SickLeaveApiController {
    private static final Logger logger = LoggerFactory.getLogger(SickLeaveApiController.class);
    private final SickLeaveService sickLeaveService;

    public SickLeaveApiController(SickLeaveService sickLeaveService) {
        this.sickLeaveService = sickLeaveService;
    }

    @Operation(summary = "Get all sick leaves", description = "Retrieves a paginated list of all sick leaves.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of all sick leaves.
     *
     * @param pageable Pagination information.
     * @return A ResponseEntity containing a Page of {@link SickLeaveViewDTO} objects.
     */
    @GetMapping
    public ResponseEntity<Page<SickLeaveViewDTO>> getAllSickLeaves(@Parameter(description = "Pagination information.") Pageable pageable) throws ExecutionException, InterruptedException {
        logger.info("API GET request for all sick leaves. Pageable: {}", pageable);
        Page<SickLeaveViewDTO> sickLeaves = sickLeaveService.getAll(pageable.getPageNumber(), pageable.getPageSize(), "startDate", false).get();
        return ResponseEntity.ok(sickLeaves);
    }

    @Operation(summary = "Get a sick leave by ID")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a single sick leave by its ID.
     *
     * @param id The ID of the sick leave to retrieve.
     * @return A ResponseEntity containing the {@link SickLeaveViewDTO}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SickLeaveViewDTO> getSickLeaveById(@Parameter(description = "The ID of the sick leave to retrieve.") @PathVariable Long id) {
        logger.info("API GET request for sick leave with ID: {}", id);
        return ResponseEntity.ok(sickLeaveService.getById(id));
    }

    @Operation(summary = "Create a new sick leave")
    @ApiResponse(responseCode = "201", description = OpenApiConstants.SUCCESS_CREATED)
    /**
     * Creates a new sick leave.
     *
     * @param createDTO The DTO containing the data for the new sick leave.
     * @return A ResponseEntity with status 201 (Created) and the created {@link SickLeaveViewDTO}.
     */
    @PostMapping
    public ResponseEntity<SickLeaveViewDTO> createSickLeave(@RequestBody @Valid SickLeaveCreateDTO createDTO) {
        logger.info("API POST request to create a new sick leave.");
        SickLeaveViewDTO createdSickLeave = sickLeaveService.create(createDTO);
        return ResponseEntity.created(URI.create("/api/sick-leaves/" + createdSickLeave.getId())).body(createdSickLeave);
    }

    @Operation(summary = "Update an existing sick leave")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Updates an existing sick leave.
     *
     * @param id The ID of the sick leave to update.
     * @param updateDTO The DTO containing the updated data.
     * @return A ResponseEntity containing the updated {@link SickLeaveViewDTO}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SickLeaveViewDTO> updateSickLeave(
            @Parameter(description = "The ID of the sick leave to update.") @PathVariable Long id,
            @RequestBody @Valid SickLeaveUpdateDTO updateDTO) {
        logger.info("API PATCH request to update sick leave with ID: {}", id);
        updateDTO.setId(id);
        SickLeaveViewDTO updatedSickLeave = sickLeaveService.update(updateDTO);
        return ResponseEntity.ok(updatedSickLeave);
    }

    @Operation(summary = "Delete a sick leave")
    @ApiResponse(responseCode = "204", description = OpenApiConstants.SUCCESS_NO_CONTENT)
    /**
     * Deletes a sick leave by its ID.
     *
     * @param id The ID of the sick leave to delete.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSickLeave(@Parameter(description = "The ID of the sick leave to delete.") @PathVariable Long id) {
        logger.info("API DELETE request for sick leave with ID: {}", id);
        sickLeaveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
