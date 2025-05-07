package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Treatment} entities.
 * Provides CRUD operations for treatment management as per the medical record system requirements.
 */
public interface TreatmentService {

    /**
     * Creates a new treatment based on the provided DTO.
     *
     * @param dto the DTO containing treatment creation data (description, visit ID, medicine IDs)
     * @return the created treatment's view DTO
     * @throws InvalidDTOException if the DTO is null or contains invalid data
     * @throws EntityNotFoundException if the visit or medicines are not found
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    TreatmentViewDTO create(TreatmentCreateDTO dto);

    /**
     * Updates an existing treatment based on the provided DTO.
     *
     * @param dto the DTO containing updated treatment data (ID, description, visit ID, medicine IDs)
     * @return the updated treatment's view DTO
     * @throws EntityNotFoundException if the treatment, visit, or medicines are not found
     * @throws InvalidDTOException if the DTO or ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    TreatmentViewDTO update(TreatmentUpdateDTO dto);

    /**
     * Deletes a treatment by ID.
     *
     * @param id the ID of the treatment to delete
     * @throws EntityNotFoundException if the treatment is not found
     * @throws InvalidDTOException if the ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    void delete(Long id);

    /**
     * Retrieves a treatment by ID.
     *
     * @param id the ID of the treatment
     * @return the treatment's view DTO
     * @throws EntityNotFoundException if the treatment is not found
     * @throws InvalidDTOException if the ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR, ROLE_PATIENT, or ROLE_ADMIN once Keycloak is configured
    TreatmentViewDTO getById(Long id);

    /**
     * Asynchronously retrieves all treatments with pagination and sorting.
     *
     * @param page     the page number (0-based)
     * @param size     the number of items per page
     * @param orderBy  the field to sort by (e.g., "description")
     * @param ascending whether to sort in ascending order
     * @return a CompletableFuture containing a page of treatment view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    CompletableFuture<Page<TreatmentViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);
}