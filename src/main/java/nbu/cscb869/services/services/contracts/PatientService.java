package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Patient} entities.
 * Provides CRUD operations and patient-related queries with role-based access control.
 */
public interface PatientService {
    /**
     * Creates a new patient with the provided DTO.
     * @param dto the DTO containing patient creation data
     * @return the created patient's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidPatientException if the EGN already exists or general practitioner is invalid
     * @throws EntityNotFoundException if the general practitioner is not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    PatientViewDTO create(PatientCreateDTO dto);

    /**
     * Updates an existing patient with the provided DTO.
     * @param dto the DTO containing updated patient data
     * @return the updated patient's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the patient or general practitioner is not found
     * @throws InvalidPatientException if the EGN is already in use or general practitioner is invalid
     */
    @PreAuthorize("hasRole('ADMIN')")
    PatientViewDTO update(PatientUpdateDTO dto);

    /**
     * Soft deletes a patient by ID.
     * @param id the ID of the patient to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the patient is not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);

    /**
     * Retrieves a patient by ID.
     * @param id the ID of the patient
     * @return the patient's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the patient is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    PatientViewDTO getById(Long id);

    /**
     * Retrieves a patient by EGN.
     * @param egn the patient's EGN
     * @return the patient's view DTO
     * @throws InvalidDTOException if the EGN is null or empty
     * @throws EntityNotFoundException if the patient is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    PatientViewDTO getByEgn(String egn);

    /**
     * Retrieves all active patients with pagination, sorting, and optional filtering by EGN.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @param filter optional filter for EGN
     * @return a CompletableFuture containing a page of patient view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves patients by general practitioner with pagination.
     * @param generalPractitionerId the ID of the general practitioner
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a page of patient view DTOs
     * @throws InvalidDTOException if the general practitioner ID is null
     * @throws EntityNotFoundException if the general practitioner is not found
     * @throws InvalidPatientException if the doctor is not a general practitioner
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size);

    /**
     * Retrieves patient counts grouped by general practitioner.
     * @return a list of DTOs containing general practitioners and their patient counts
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner();
}