package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.identity.RoleCreateDTO;
import nbu.cscb869.services.data.dtos.identity.RoleUpdateDTO;
import nbu.cscb869.services.data.dtos.identity.RoleViewDTO;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.identity.Role} entities.
 * Provides CRUD operations for role management with Keycloak integration.
 */
public interface RoleService {
    /**
     * Creates a new role with the provided DTO and synchronizes with Keycloak.
     * @param dto the DTO containing role creation data
     * @return the created role's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidDTOException if the role name already exists
     */
    RoleViewDTO create(RoleCreateDTO dto);

    /**
     * Updates an existing role with the provided DTO and synchronizes with Keycloak.
     * @param dto the DTO containing updated role data
     * @return the updated role's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the role is not found
     * @throws InvalidDTOException if the role name already exists
     */
    RoleViewDTO update(RoleUpdateDTO dto);

    /**
     * Soft deletes a role by ID and removes from Keycloak.
     * @param id the ID of the role to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the role is not found
     */
    void delete(Long id);

    /**
     * Retrieves a role by ID.
     * @param id the ID of the role
     * @return the role's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the role is not found
     */
    RoleViewDTO getById(Long id);

    /**
     * Retrieves all active roles with pagination and sorting.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @return a CompletableFuture containing a page of role view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    CompletableFuture<Page<RoleViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);
}