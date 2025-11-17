package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.services.data.dtos.PatientVisitScheduleDTO;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitUpdateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * RESTful API Controller for managing Visit entities.
 */
@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visit API", description = "Endpoints for managing patient visits.")
@ApiStandardResponses
public class VisitApiController {

    private static final Logger logger = LoggerFactory.getLogger(VisitApiController.class);
    private final VisitService visitService;

    public VisitApiController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Get all visits", description = "Retrieves a paginated list of all visits. Restricted to ADMIN and DOCTOR roles.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of all visits.
     * Accessible by users with 'ADMIN' or 'DOCTOR' roles.
     *
     * @param pageable Pagination information.
     * @param filter Optional filter for searching visits.
     * @return A ResponseEntity containing a Page of {@link VisitViewDTO} objects.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Page<VisitViewDTO>> getAllVisits(
            @Parameter(description = "Pagination information.") Pageable pageable,
            @Parameter(description = "Optional filter for patient EGN or doctor Unique ID.") @RequestParam(required = false) String filter) throws ExecutionException, InterruptedException {
        logger.info("API GET request for all visits. Pageable: {}, Filter: {}", pageable, filter);
        Page<VisitViewDTO> visits = visitService.getAll(pageable.getPageNumber(), pageable.getPageSize(), "visitDate", false, filter).get();
        return ResponseEntity.ok(visits);
    }

    @Operation(summary = "Get a single visit by ID")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a single visit by its ID.
     * Accessible by 'ADMIN', 'DOCTOR', or the 'PATIENT' involved in the visit.
     *
     * @param id The ID of the visit to retrieve.
     * @return A ResponseEntity containing the {@link VisitViewDTO}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR') or @visitServiceImpl.getById(#id).patient.keycloakId == authentication.name")
    public ResponseEntity<VisitViewDTO> getVisitById(@Parameter(description = "The ID of the visit to retrieve.") @PathVariable Long id) {
        logger.info("API GET request for visit with ID: {}", id);
        return ResponseEntity.ok(visitService.getById(id));
    }

    @Operation(summary = "Create a new visit (Admin/Doctor)", description = "Allows an Admin or Doctor to create a new visit record.")
    @ApiResponse(responseCode = "201", description = OpenApiConstants.SUCCESS_CREATED)
    /**
     * Creates a new visit.
     * This endpoint is restricted to users with the 'DOCTOR' or 'ADMIN' role.
     *
     * @param visitCreateDTO The DTO containing the data for the new visit.
     * @return A ResponseEntity with status 201 (Created) and the created {@link VisitViewDTO}.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<VisitViewDTO> createVisit(@RequestBody @Valid VisitCreateDTO visitCreateDTO) {
        logger.info("API POST request to create a new visit.");
        VisitViewDTO createdVisit = visitService.create(visitCreateDTO);
        return ResponseEntity.created(URI.create("/api/visits/" + createdVisit.getId())).body(createdVisit);
    }

    @Operation(summary = "Schedule a new visit (Patient)", description = "Allows a patient to schedule a new visit for themselves.")
    @ApiResponse(responseCode = "201", description = OpenApiConstants.SUCCESS_CREATED)
    /**
     * Allows a patient to schedule a new visit for themselves.
     *
     * @param scheduleDTO The DTO containing the scheduling details.
     * @param authentication The current user's authentication object.
     * @return A ResponseEntity with status 201 (Created) and the created {@link VisitViewDTO}.
     */
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<VisitViewDTO> scheduleVisitByPatient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "DTO with doctor ID, date, and time.")
            @RequestBody @Valid PatientVisitScheduleDTO scheduleDTO, Authentication authentication) {
        logger.info("API POST request for patient to schedule a visit.");
        String userKeycloakId = authentication.getName();
        VisitViewDTO createdVisit = visitService.scheduleNewVisitForUser(userKeycloakId, scheduleDTO);
        return ResponseEntity.created(URI.create("/api/visits/" + createdVisit.getId())).body(createdVisit);
    }

    @Operation(summary = "Update a visit", description = "Partially updates an existing visit. Accessible by ADMIN or the assigned DOCTOR.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Partially updates an existing visit.
     * Accessible by 'ADMIN' or the 'DOCTOR' who conducted the visit.
     *
     * @param id The ID of the visit to update.
     * @param visitUpdateDTO The DTO containing the fields to update.
     * @return A ResponseEntity containing the updated {@link VisitViewDTO}.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @visitServiceImpl.getById(#id).doctor.keycloakId == authentication.name")
    public ResponseEntity<VisitViewDTO> updateVisit(
            @Parameter(description = "The ID of the visit to update.") @PathVariable Long id,
            @RequestBody @Valid VisitUpdateDTO visitUpdateDTO) {
        logger.info("API PATCH request to update visit with ID: {}", id);
        visitUpdateDTO.setId(id);
        VisitViewDTO updatedVisit = visitService.update(visitUpdateDTO);
        return ResponseEntity.ok(updatedVisit);
    }

    @Operation(summary = "Cancel a visit (Patient)", description = "Allows a patient to cancel their own scheduled visit.")
    @ApiResponse(responseCode = "204", description = OpenApiConstants.SUCCESS_NO_CONTENT)
    /**
     * Cancels a scheduled visit.
     * Accessible only by the patient who owns the visit.
     * @param id The ID of the visit to cancel.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> cancelVisit(@Parameter(description = "The ID of the visit to cancel.") @PathVariable Long id) {
        logger.info("API POST request to cancel visit with ID: {}", id);
        visitService.cancelVisit(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a visit (Admin)")
    @ApiResponse(responseCode = "204", description = OpenApiConstants.SUCCESS_NO_CONTENT)
    /**
     * Deletes a visit by its ID.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the visit to delete.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVisit(@Parameter(description = "The ID of the visit to delete.") @PathVariable Long id) {
        logger.info("API DELETE request for visit with ID: {}", id);
        visitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
