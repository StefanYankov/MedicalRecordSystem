package nbu.cscb869.web.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * RESTful API Controller for managing Patient entities.
 */
@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient API", description = "Endpoints for managing patient records.")
public class PatientApiController {
    private static final Logger logger = LoggerFactory.getLogger(PatientApiController.class);
    private final PatientService patientService;

    public PatientApiController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Get all patients", description = "Retrieves a paginated list of all patients. Restricted to ADMIN and DOCTOR roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of patients"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN or DOCTOR role")
    })
    /**
     * Retrieves a paginated list of all patients.
     * Restricted to users with 'ADMIN' or 'DOCTOR' roles.
     *
     * @param pageable Pagination information.
     * @param keyword  Optional search keyword for EGN.
     * @return A ResponseEntity containing a Page of {@link PatientViewDTO} objects.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Page<PatientViewDTO>> getAllPatients(
            @Parameter(description = "Pagination information (page, size, sort)") Pageable pageable,
            @Parameter(description = "Optional filter for searching by patient EGN") @RequestParam(required = false) String keyword) {
        logger.info("API GET request for all patients. Pageable: {}, Keyword: {}", pageable, keyword);
        return ResponseEntity.ok(patientService.findAll(pageable, keyword));
    }

    @Operation(summary = "Get a patient by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the patient"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Patient is trying to access another patient's record"),
            @ApiResponse(responseCode = "404", description = "Not Found - No patient found with the given ID")
    })
    /**
     * Retrieves a single patient by their ID.
     * Admins and Doctors can retrieve any patient.
     * Patients can only retrieve their own profile.
     *
     * @param id The ID of the patient to retrieve.
     * @return A ResponseEntity containing the {@link PatientViewDTO}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR') or @patientServiceImpl.getById(#id).keycloakId == authentication.name")
    public ResponseEntity<PatientViewDTO> getPatientById(
            @Parameter(description = "The ID of the patient to retrieve") @PathVariable Long id) {
        logger.info("API GET request for patient with ID: {}", id);
        return ResponseEntity.ok(patientService.getById(id));
    }

    @Operation(summary = "Create a new patient (Admin)", description = "Allows an Admin to create a new patient record from scratch.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input - EGN or Keycloak ID might already exist"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only users with ADMIN role can use this endpoint")
    })
    /**
     * Creates a new patient.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param createDTO The DTO containing the data for the new patient.
     * @return A ResponseEntity with status 201 (Created) and the created {@link PatientViewDTO}.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientViewDTO> createPatient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "DTO with all patient details, including Keycloak ID.")
            @RequestBody @Valid PatientCreateDTO createDTO) {
        logger.info("API POST request to create a new patient.");
        PatientViewDTO createdPatient = patientService.create(createDTO);
        return ResponseEntity.created(URI.create("/api/patients/" + createdPatient.getId())).body(createdPatient);
    }

    @Operation(summary = "Register a new patient profile (Patient)", description = "Links a Keycloak user to a new patient profile in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient profile successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input - EGN might already exist or GP ID is invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only users with PATIENT role can register a patient profile")
    })
    /**
     * Allows a currently authenticated user to register as a patient.
     * The keycloakId is taken from the security context.
     *
     * @param createDTO The DTO containing the patient's EGN and chosen GP.
     * @return A ResponseEntity with status 201 (Created) and the created {@link PatientViewDTO}.
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientViewDTO> registerPatient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "DTO containing EGN and chosen General Practitioner ID.")
            @RequestBody @Valid PatientCreateDTO createDTO) {
        logger.info("API POST request for patient self-registration.");
        PatientViewDTO registeredPatient = patientService.registerPatient(createDTO);
        return ResponseEntity.created(URI.create("/api/patients/" + registeredPatient.getId())).body(registeredPatient);
    }

    @Operation(summary = "Update a patient (Admin)", description = "Partially updates an existing patient's details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient successfully updated"),
            @ApiResponse(responseCode = "404", description = "Not Found - No patient found with the given ID")
    })
    /**
     * Partially updates an existing patient.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the patient to update.
     * @param updateDTO The DTO containing the fields to update.
     * @return A ResponseEntity containing the updated {@link PatientViewDTO}.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientViewDTO> updatePatient(
            @Parameter(description = "The ID of the patient to update") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "DTO with the fields to be updated.")
            @RequestBody PatientUpdateDTO updateDTO) {
        logger.info("API PATCH request to update patient with ID: {}", id);
        updateDTO.setId(id);
        return ResponseEntity.ok(patientService.update(updateDTO));
    }

    @Operation(summary = "Update patient insurance status (Admin)", description = "Sets the patient's last insurance payment date to the current date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Insurance status successfully updated"),
            @ApiResponse(responseCode = "404", description = "Not Found - No patient found with the given ID")
    })
    /**
     * Updates the insurance status of a patient.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the patient to update.
     * @return A ResponseEntity containing the updated {@link PatientViewDTO}.
     */
    @PostMapping("/{id}/update-insurance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientViewDTO> updateInsuranceStatus(
            @Parameter(description = "The ID of the patient whose insurance is to be updated") @PathVariable Long id) {
        logger.info("API POST request to update insurance for patient with ID: {}", id);
        return ResponseEntity.ok(patientService.updateInsuranceStatus(id));
    }

    @Operation(summary = "Delete a patient (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Patient successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Not Found - No patient found with the given ID")
    })
    /**
     * Deletes a patient by their ID.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param id The ID of the patient to delete.
     * @return A ResponseEntity with status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @Parameter(description = "The ID of the patient to delete") @PathVariable Long id) {
        logger.info("API DELETE request for patient with ID: {}", id);
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
