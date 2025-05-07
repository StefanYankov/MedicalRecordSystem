package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.SickLeave} entities.
 * Provides CRUD operations for sick leave management as per the medical record system requirements.
 */
public interface SickLeaveService {

    /**
     * Creates a new sick leave based on the provided DTO.
     *
     * @param dto the DTO containing sick leave creation data (start date, duration, visit ID)
     * @return the created sick leave's view DTO
     * @throws InvalidDTOException if the DTO is null or contains invalid data
     * @throws EntityNotFoundException if the associated visit is not found
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    SickLeaveViewDTO create(SickLeaveCreateDTO dto);

    /**
     * Updates an existing sick leave based on the provided DTO.
     *
     * @param dto the DTO containing updated sick leave data (ID, start date, duration, visit ID)
     * @return the updated sick leave's view DTO
     * @throws EntityNotFoundException if the sick leave or visit is not found
     * @throws InvalidDTOException if the DTO or ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    SickLeaveViewDTO update(SickLeaveUpdateDTO dto);

    /**
     * Deletes a sick leave by ID.
     *
     * @param id the ID of the sick leave to delete
     * @throws EntityNotFoundException if the sick leave is not found
     * @throws InvalidDTOException if the ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    void delete(Long id);

    /**
     * Retrieves a sick leave by ID.
     *
     * @param id the ID of the sick leave
     * @return the sick leave's view DTO
     * @throws EntityNotFoundException if the sick leave is not found
     * @throws InvalidDTOException if the ID is null
     */
    // TODO: Restrict to ROLE_DOCTOR, ROLE_PATIENT, or ROLE_ADMIN once Keycloak is configured
    SickLeaveViewDTO getById(Long id);

    /**
     * Asynchronously retrieves all sick leaves with pagination and sorting.
     *
     * @param page     the page number (0-based)
     * @param size     the number of items per page
     * @param orderBy  the field to sort by (e.g., "startDate")
     * @param ascending whether to sort in ascending order
     * @return a CompletableFuture containing a page of sick leave view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Keycloak is configured
    CompletableFuture<Page<SickLeaveViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);
}