package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Diagnosis} entities.
 * Provides CRUD operations and diagnosis-related queries with role-based access control.
 */
public interface DiagnosisService {
    /**
     * Creates a new diagnosis with the provided DTO.
     * @param dto the DTO containing diagnosis creation data
     * @return the created diagnosis's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidDTOException if the diagnosis name already exists
     */
    @PreAuthorize("hasRole('ADMIN')")
    DiagnosisViewDTO create(DiagnosisCreateDTO dto);

    /**
     * Updates an existing diagnosis with the provided DTO.
     * @param dto the DTO containing updated diagnosis data
     * @return the updated diagnosis's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the diagnosis is not found
     * @throws InvalidDTOException if the diagnosis name already exists
     */
    @PreAuthorize("hasRole('ADMIN')")
    DiagnosisViewDTO update(DiagnosisUpdateDTO dto);

    /**
     * Soft deletes a diagnosis by ID.
     * @param id the ID of the diagnosis to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the diagnosis is not found
     * @throws InvalidDTOException if the diagnosis is referenced in active visits
     */
    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);

    /**
     * Retrieves a diagnosis by ID.
     * @param id the ID of the diagnosis
     * @return the diagnosis's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the diagnosis is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    DiagnosisViewDTO getById(Long id);

    /**
     * Retrieves a diagnosis by name.
     * @param name the name of the diagnosis
     * @return the diagnosis's view DTO
     * @throws InvalidDTOException if the name is null or empty
     * @throws EntityNotFoundException if the diagnosis is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    DiagnosisViewDTO getByName(String name);

    /**
     * Retrieves all active diagnoses with pagination, sorting, and optional filtering.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @param filter optional filter for diagnosis name
     * @return a CompletableFuture containing a page of diagnosis view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<DiagnosisViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves patients diagnosed with a specific diagnosis.
     * @param diagnosisId the ID of the diagnosis
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a page of patient diagnosis DTOs
     * @throws InvalidDTOException if the diagnosis ID is null
     * @throws EntityNotFoundException if the diagnosis is not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    Page<PatientDiagnosisDTO> getPatientsByDiagnosis(Long diagnosisId, int page, int size);

    /**
     * Retrieves the most frequently diagnosed conditions.
     * @return a list of DTOs with diagnoses and their visit counts
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DiagnosisVisitCountDTO> getMostFrequentDiagnoses();
}