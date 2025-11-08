package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityInUseException;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Specialty} entities.
 * Provides administrative CRUD operations for medical specialties.
 */
public interface SpecialtyService {

    /**
     * Creates a new specialty.
     *
     * @param dto The DTO containing the data for the new specialty.
     * @return A view DTO of the newly created specialty.
     * @throws InvalidDTOException if the DTO is null or the specialty name already exists.
     */
    SpecialtyViewDTO create(SpecialtyCreateDTO dto);

    /**
     * Updates an existing specialty.
     *
     * @param dto The DTO containing the updated data.
     * @return A view DTO of the updated specialty.
     * @throws InvalidDTOException if the DTO or its ID is null, or if the name is taken by another specialty.
     * @throws EntityNotFoundException if no specialty with the given ID is found.
     */
    SpecialtyViewDTO update(SpecialtyUpdateDTO dto);

    /**
     * Deletes a specialty by its ID.
     *
     * @param id The ID of the specialty to delete.
     * @throws EntityNotFoundException if no specialty with the given ID is found.
     * @throws EntityInUseException if the specialty is still assigned to one or more doctors.
     */
    void delete(Long id);

    /**
     * Retrieves a specialty by its ID.
     *
     * @param id The ID of the specialty to retrieve.
     * @return A view DTO of the specialty.
     * @throws EntityNotFoundException if no specialty with the given ID is found.
     */
    SpecialtyViewDTO getById(Long id);

    /**
     * Asynchronously retrieves a paginated list of all specialties.
     *
     * @param page      The page number (0-based).
     * @param size      The number of items per page.
     * @param orderBy   The field to sort by (e.g., "name").
     * @param ascending Whether to sort in ascending order.
     * @return A CompletableFuture containing a page of specialty view DTOs.
     */
    CompletableFuture<Page<SpecialtyViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);
}
