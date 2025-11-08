package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Patient} entities.
 * Provides CRUD operations and patient-related queries.
 */
public interface PatientService {
    /**
     * Creates a new patient with the provided DTO. For Admin use.
     * Note: The PatientCreateDTO must be updated to include a keycloakId.
     * @param dto the DTO containing patient creation data
     * @return the created patient's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidPatientException if the EGN already exists or general practitioner is invalid.
     * @throws EntityNotFoundException if the general practitioner is not found.
     */
    @PreAuthorize("hasRole('ADMIN')")
    PatientViewDTO create(PatientCreateDTO dto);

    /**
     * Allows a patient to register themselves.
     * The keycloakId is taken from the security context.
     * @param dto the DTO containing patient creation data.
     * @return the created patient's view DTO.
     */
    @PreAuthorize("hasRole('PATIENT')")
    PatientViewDTO registerPatient(PatientCreateDTO dto);

    /**
     * Updates an existing patient with the provided DTO.
     * @param dto the DTO containing updated patient data.
     * @return the updated patient's view DTO.
     * @throws InvalidDTOException if the DTO or ID is null.
     * @throws EntityNotFoundException if the patient or general practitioner is not found.
     * @throws InvalidPatientException if the EGN is already in use or general practitioner is invalid.
     */
    PatientViewDTO update(PatientUpdateDTO dto);

    /**
     * Updates the insurance status of a patient to the current date.
     * This is an administrative action to simulate a successful insurance check.
     *
     * @param patientId The ID of the patient to update.
     * @return The updated patient's view DTO.
     */
    @PreAuthorize("hasRole('ADMIN')")
    PatientViewDTO updateInsuranceStatus(Long patientId);

    /**
     * Deletes a patient by ID.
     * @param id the ID of the patient to delete.
     * @throws InvalidDTOException if the ID is null.
     * @throws EntityNotFoundException if the patient is not found.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);

    /**
     * Retrieves a patient by ID.
     * @param id the ID of the patient.
     * @return the patient's view DTO.
     * @throws InvalidDTOException if the ID is null.
     * @throws EntityNotFoundException if the patient is not found.
     */
    PatientViewDTO getById(Long id);

    /**
     * Retrieves a patient by EGN.
     * @param egn the patient's EGN.
     * @return the patient's view DTO.
     * @throws InvalidDTOException if the EGN is null or empty.
     * @throws EntityNotFoundException if the patient is not found.
     */
    PatientViewDTO getByEgn(String egn);

    /**
     * Retrieves a patient by their Keycloak ID.
     * @param keycloakId the user's Keycloak ID.
     * @return the patient's view DTO.
     * @throws InvalidDTOException if the keycloakId is null or empty.
     * @throws EntityNotFoundException if the patient is not found.
     */
    PatientViewDTO getByKeycloakId(String keycloakId);

    /**
     * Retrieves all active patients with pagination, sorting, and optional filtering by EGN.
     * This method is intended for Admin use.
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @param orderBy the field to sort by.
     * @param ascending whether to sort in ascending order.
     * @param filter optional filter for EGN.
     * @return a CompletableFuture containing a page of patient view DTOs.
     * @throws InvalidDTOException if pagination parameters are invalid.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Async
    CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves all patients, optionally filtered by a keyword.
     * This method is intended for Admin and Doctor use.
     * @param pageable Pagination information.
     * @param keyword The keyword to filter by (name or EGN).
     * @return A page of patients.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<PatientViewDTO> findAll(Pageable pageable, String keyword);

    /**
     * Retrieves patients by general practitioner with pagination.
     * @param generalPractitionerId the ID of the general practitioner.
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @return a page of patient view DTOs.
     * @throws InvalidDTOException if the general practitioner ID is null.
     * @throws EntityNotFoundException if the general practitioner is not found.
     * @throws InvalidPatientException if the doctor is not a general practitioner.
     */
    @PreAuthorize("hasRole('ADMIN')")
    Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size);

    /**
     * Retrieves patient counts grouped by general practitioner.
     * @return a list of DTOs containing general practitioners and their patient counts.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner();

    /**
     * Retrieves a page of patients associated with a specific doctor.
     * This includes patients for whom the doctor is the general practitioner,
     * or patients who have had at least one visit with this doctor.
     *
     * @param doctorId The ID of the doctor.
     * @param pageable Pagination information.
     * @return A page of PatientViewDTOs.
     */
    @PreAuthorize("hasRole('DOCTOR')")
    Page<PatientViewDTO> findPatientsForDoctor(Long doctorId, Pageable pageable);

    /**
     * Checks if a specific patient is associated with a given doctor.
     * A patient is considered associated if the doctor is their general practitioner
     * or if the patient has had at least one visit with this doctor.
     *
     * @param patientId The ID of the patient.
     * @param doctorId The ID of the doctor.
     * @return true if the patient is associated with the doctor, false otherwise.
     */
    boolean isPatientAssociatedWithDoctor(Long patientId, Long doctorId);
}
