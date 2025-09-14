package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.services.data.dtos.identity.UserCreateDTO;
import nbu.cscb869.services.data.dtos.identity.UserUpdateDTO;
import nbu.cscb869.services.data.dtos.identity.UserViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.identity.User} entities.
 * Provides CRUD operations, role switching, and Keycloak integration.
 */
public interface UserService {
    /**
     * Creates a new user with the provided DTO and synchronizes with Keycloak.
     * @param dto the DTO containing user creation data
     * @return the created user's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidInputException if the Keycloak ID or email already exists
     */
    UserViewDTO create(UserCreateDTO dto);

    /**
     * Updates an existing user with the provided DTO and synchronizes with Keycloak.
     * @param dto the DTO containing updated user data
     * @return the updated user's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the user is not found
     * @throws InvalidInputException if the Keycloak ID or email is already in use
     */
    UserViewDTO update(UserUpdateDTO dto);

    /**
     * Soft deletes a user by ID and removes from Keycloak.
     * @param id the ID of the user to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the user is not found
     */
    void delete(Long id);

    /**
     * Retrieves a user by ID.
     * @param id the ID of the user
     * @return the user's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the user is not found
     */
    UserViewDTO getById(Long id);

    /**
     * Retrieves a user by Keycloak ID.
     * @param keycloakId the Keycloak user ID
     * @return the user's view DTO
     * @throws InvalidDTOException if the Keycloak ID is null
     * @throws EntityNotFoundException if the user is not found
     */
    UserViewDTO getByKeycloakId(String keycloakId);

    /**
     * Retrieves all active users with pagination and sorting.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @return a CompletableFuture containing a page of user view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    CompletableFuture<Page<UserViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);

    /**
     * Switches the active role of a user in Keycloak.
     * @param keycloakId the Keycloak user ID
     * @param newRole the role to switch to (e.g., DOCTOR, PATIENT)
     * @throws EntityNotFoundException if the user or role is not found
     * @throws InvalidInputException if the role is invalid or restricted
     */
    void switchRole(String keycloakId, String newRole);

    /**
     * Retrieves the current user's Keycloak ID from the JWT.
     * @param jwt the JWT containing user information
     * @return the Keycloak user ID
     * @throws InvalidInputException if the JWT is null
     */
    String getCurrentUserId(Jwt jwt);
}