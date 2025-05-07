package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Diagnosis} entities.
 * Provides CRUD operations for diagnosis management as per the medical record system requirements.
 */
public interface DiagnosisService {

    /**
     * Creates a new diagnosis based on the provided DTO.
     *
     * @param dto the DTO containing diagnosis creation data (name, description)
     * @return the created diagnosis's view DTO
     * @throws InvalidDTOException if the DTO is null or contains invalid data
     */
    // TODO: Restrict to ROLE_ADMIN once Keycloak is configured
    DiagnosisViewDTO create(DiagnosisCreateDTO dto);

    /**
     * Updates an existing diagnosis based on the provided DTO.
     *
     * @param dto the DTO containing updated diagnosis data (ID, name, description)
     * @return the updated diagnosis's view DTO
     * @throws EntityNotFoundException if the diagnosis is not found
     * @throws InvalidDTOException if the DTO or ID is null
     */
    // TODO: Restrict to ROLE_ADMIN once Keycloak is configured
    DiagnosisViewDTO update(DiagnosisUpdateDTO dto);

    /**
     * Deletes a diagnosis by ID (soft delete).
     *
     * @param id the ID of the diagnosis to delete
     * @throws EntityNotFoundException if the diagnosis is not found
     * @throws InvalidDTOException if the ID is null
     */
    // TODO: Restrict to ROLE_ADMIN once Keycloak is configured
    void delete(Long id);

    /**
     * Retrieves a diagnosis by ID.
     *
     * @param id the ID of the diagnosis
     * @return the diagnosis's view DTO
     * @throws EntityNotFoundException if the diagnosis is not found
     * @throws InvalidDTOException if the ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    DiagnosisViewDTO getById(Long id);

    /**
     * Asynchronously retrieves all diagnoses with pagination, sorting, and optional filtering.
     *
     * @param page     the page number (0-based)
     * @param size     the number of items per page
     * @param orderBy  the field to sort by (e.g., "name")
     * @param ascending whether to sort in ascending order
     * @param filter   optional filter string to match against name or description
     * @return a CompletableFuture containing a page of diagnosis view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    CompletableFuture<Page<DiagnosisViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);
}