package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.identity.UserRoleAssignmentCreateDTO;
import nbu.cscb869.services.data.dtos.identity.UserRoleAssignmentUpdateDTO;
import nbu.cscb869.services.data.dtos.identity.UserRoleAssignmentViewDTO;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.identity.UserRoleAssignment} entities.
 * Provides CRUD operations for user role assignments with Keycloak integration.
 */
public interface UserRoleAssignmentService {
    /**
     * Creates a new user role assignment with the provided DTO and synchronizes with Keycloak.
     * @param dto the DTO containing assignment creation data
     * @return the created assignment's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws EntityNotFoundException if the user or role is not found
     * @throws InvalidDTOException if the assignment already exists
     */
    UserRoleAssignmentViewDTO create(UserRoleAssignmentCreateDTO dto);

    /**
     * Updates an existing user role assignment with the provided DTO and synchronizes with Keycloak.
     * @param dto the DTO containing updated assignment data
     * @return the updated assignment's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the assignment, user, or role is not found
     * @throws InvalidDTOException if the updated assignment already exists
     */
    UserRoleAssignmentViewDTO update(UserRoleAssignmentUpdateDTO dto);

    /**
     * Soft deletes a user role assignment by ID and removes from Keycloak.
     * @param id the ID of the assignment to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the assignment is not found
     */
    void delete(Long id);

    /**
     * Retrieves a user role assignment by ID.
     * @param id the ID of the assignment
     * @return the assignment's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the assignment is not found
     */
    UserRoleAssignmentViewDTO getById(Long id);

    /**
     * Retrieves all active role assignments for a user with pagination and sorting.
     * @param userId the ID of the user
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @return a CompletableFuture containing a page of assignment view DTOs
     * @throws InvalidDTOException if parameters are invalid
     * @throws EntityNotFoundException if the user is not found
     */
    CompletableFuture<Page<UserRoleAssignmentViewDTO>> getAllByUser(Long userId, int page, int size, String orderBy, boolean ascending);
}