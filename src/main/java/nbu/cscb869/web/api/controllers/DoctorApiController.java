package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nbu.cscb869.config.OpenApiConstants;
import nbu.cscb869.config.annotations.ApiStandardResponses;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * RESTful API Controller for managing Doctor entities.
 */
@RestController
@RequestMapping("/api/doctors")
@Tag(name = "Doctor API", description = "Endpoints for managing doctor records.")
@ApiStandardResponses
public class DoctorApiController {
    private static final Logger logger = LoggerFactory.getLogger(DoctorApiController.class);
    private final DoctorService doctorService;

    /**
     * Constructs the controller with the necessary DoctorService.
     * @param doctorService The service for doctor-related operations.
     */
    public DoctorApiController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Operation(summary = "Get all doctors", description = "Retrieves a paginated list of all doctors. Accessible by any authenticated user.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of all doctors.
     * Accessible by any authenticated user.
     *
     * @param pageable Pagination information.
     * @param filter Optional filter for searching by unique ID number.
     * @return A ResponseEntity containing a Page of {@link DoctorViewDTO} objects.
     * @throws ExecutionException If an error occurs during asynchronous execution.
     * @throws InterruptedException If the thread is interrupted during asynchronous execution.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DoctorViewDTO>> getAllDoctors(
            @Parameter(description = "Pagination information (e.g., page, size, sort)") Pageable pageable,
            @Parameter(description = "Optional filter for searching by unique ID number") @RequestParam(required = false) String filter) throws ExecutionException, InterruptedException {
        logger.info("API GET request for all doctors. Pageable: {}, Filter: {}", pageable, filter);
        Page<DoctorViewDTO> doctors = doctorService.getAllAsync(pageable.getPageNumber(), pageable.getPageSize(), "name", true, filter).get();
        return ResponseEntity.ok(doctors);
    }

    @Operation(summary = "Get a single doctor by ID")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a single doctor by their ID.
     * Accessible by any authenticated user.
     *
     * @param id The ID of the doctor to retrieve.
     * @return A ResponseEntity containing the {@link DoctorViewDTO}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorViewDTO> getDoctorById(@Parameter(description = "The ID of the doctor to retrieve.") @PathVariable Long id) {
        logger.info("API GET request for doctor with ID: {}", id);
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @Operation(summary = "Get all unapproved doctors", description = "Retrieves a paginated list of doctors awaiting admin approval. Restricted to ADMIN role.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Retrieves a paginated list of unapproved doctors.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param pageable Pagination information.
     * @return A ResponseEntity containing a Page of {@link DoctorViewDTO} objects.
     */
    @GetMapping("/unapproved")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DoctorViewDTO>> getUnapprovedDoctors(@Parameter(description = "Pagination information (e.g., page, size, sort)") Pageable pageable) {
        logger.info("API GET request for unapproved doctors. Pageable: {}", pageable);
        return ResponseEntity.ok(doctorService.getUnapprovedDoctors(pageable.getPageNumber(), pageable.getPageSize()));
    }

    @Operation(summary = "Create a new doctor (Admin)", description = "Allows an Admin to create a new doctor record.")
    @ApiResponse(responseCode = "201", description = OpenApiConstants.SUCCESS_CREATED)
    /**
     * Creates a new doctor.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param createDTO The DTO containing the data for the new doctor.
     * @return A ResponseEntity with status 201 (Created) and the created {@link DoctorViewDTO}.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorViewDTO> createDoctor(@RequestBody @Valid DoctorCreateDTO createDTO) {
        logger.info("API POST request to create a new doctor.");
        DoctorViewDTO createdDoctor = doctorService.createDoctor(createDTO);
        return ResponseEntity.created(URI.create("/api/doctors/" + createdDoctor.getId())).body(createdDoctor);
    }

    @Operation(summary = "Update a doctor (Admin)", description = "Partially updates an existing doctor's details, including approval status.")
    @ApiResponse(responseCode = "200", description = OpenApiConstants.SUCCESS_OK)
    /**
     * Partially updates an existing doctor.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the doctor to update.
     * @param updateDTO The DTO containing the fields to update.
     * @return A ResponseEntity containing the updated {@link DoctorViewDTO}.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorViewDTO> updateDoctor(
            @Parameter(description = "The ID of the doctor to update.") @PathVariable Long id,
            @RequestBody @Valid DoctorUpdateDTO updateDTO) {
        logger.info("API PATCH request to update doctor with ID: {}", id);
        updateDTO.setId(id);
        return ResponseEntity.ok(doctorService.updateDoctor(updateDTO));
    }

    @Operation(summary = "Delete a doctor (Admin)")
    @ApiResponse(responseCode = "204", description = OpenApiConstants.SUCCESS_NO_CONTENT)
    /**
     * Deletes a doctor by their ID.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the doctor to delete.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDoctor(@Parameter(description = "The ID of the doctor to delete.") @PathVariable Long id) {
        logger.info("API DELETE request for doctor with ID: {}", id);
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload doctor profile image (Admin)")
    @ApiResponse(responseCode = "200", description = "Image uploaded successfully.")
    /**
     * Uploads or updates a profile image for a doctor.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the doctor.
     * @param file The image file to upload.
     * @return A ResponseEntity containing the updated {@link DoctorViewDTO}.
     */
    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorViewDTO> uploadDoctorImage(
            @Parameter(description = "The ID of the doctor.") @PathVariable Long id,
            @Parameter(description = "The image file to upload.") @RequestParam("file") MultipartFile file) {
        logger.info("API POST request to upload image for doctor with ID: {}", id);
        DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
        updateDTO.setId(id);
        return ResponseEntity.ok(doctorService.update(updateDTO, file));
    }
}
