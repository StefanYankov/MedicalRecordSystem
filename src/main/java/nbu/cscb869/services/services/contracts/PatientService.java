package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.common.exceptions.InvalidPatientException;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing patient-related operations in the Medical Record System.
 * Provides methods for creating, updating, deleting, and retrieving patient data,
 * as well as querying patients by general practitioner and counting patients per general practitioner.
 */
public interface PatientService {

    /**
     * Creates a new patient based on the provided DTO.
     *
     * @param createDTO the DTO containing patient details
     * @return the created patient as a view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidPatientException if the EGN is already used or the general practitioner is invalid
     */
    PatientViewDTO create(PatientCreateDTO createDTO);

    /**
     * Updates an existing patient based on the provided DTO.
     *
     * @param updateDTO the DTO containing updated patient details
     * @return the updated patient as a view DTO
     * @throws InvalidDTOException if the DTO is null, ID is null, or invalid
     * @throws EntityNotFoundException if the patient or general practitioner is not found
     */
    PatientViewDTO update(PatientUpdateDTO updateDTO);

    /**
     * Soft deletes a patient by ID.
     *
     * @param id the ID of the patient to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the patient is not found
     */
    void delete(Long id);

    /**
     * Retrieves a patient by ID.
     *
     * @param id the ID of the patient
     * @return the patient as a view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the patient is not found
     */
    PatientViewDTO getById(Long id);

    /**
     * Retrieves a patient by EGN.
     *
     * @param egn the EGN of the patient
     * @return the patient as a view DTO
     * @throws InvalidDTOException if the EGN is null or empty
     * @throws EntityNotFoundException if the patient is not found
     */
    PatientViewDTO getByEgn(String egn);

    /**
     * Retrieves a paginated list of all non-deleted patients, optionally filtered.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @param orderBy the field to order by
     * @param ascending whether to sort in ascending order
     * @param filter optional filter string to match against name or EGN
     * @return a completable future containing a page of patient view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves a paginated list of non-deleted patients assigned to a specific general practitioner.
     *
     * @param generalPractitionerId the ID of the general practitioner
     * @param pageable pagination information
     * @return a page of patient view DTOs
     * @throws EntityNotFoundException if the general practitioner is not found
     */
    Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, Pageable pageable);

    /**
     * Retrieves a list of general practitioners with their patient counts.
     *
     * @return a list of DTOs containing doctors and their patient counts
     */
    List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner();
}