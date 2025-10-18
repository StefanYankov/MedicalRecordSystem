package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Treatment} entities.
 * Provides CRUD operations for treatment management as per the medical record system requirements.
 * <p>
 * DESIGN CHOICE: This service manages the entire lifecycle of the Treatment aggregate,
 * including its child {@link nbu.cscb869.data.models.Medicine} entities. The Medicine entity
 * cannot exist without a parent Treatment, so its persistence is handled here, not in a separate MedicineService.
 */
public interface TreatmentService {

    /**
     * Creates a new treatment and its associated medicines.
     *
     * @param dto the DTO containing treatment data and a list of medicine data to be created.
     * @return the created treatment's view DTO, including its medicines.
     * @throws InvalidDTOException if the DTO is null or contains invalid data.
     * @throws EntityNotFoundException if the associated visit is not found.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    TreatmentViewDTO create(TreatmentCreateDTO dto);

    /**
     * Updates an existing treatment, including its list of medicines.
     * The list of medicines in the DTO is considered the source of truth; existing medicines
     * not in the list will be removed, new medicines will be added, and existing ones will be updated.
     *
     * @param dto the DTO containing updated treatment data and the complete list of medicines.
     * @return the updated treatment's view DTO.
     * @throws EntityNotFoundException if the treatment or its associated visit is not found.
     * @throws InvalidDTOException if the DTO or its ID is null.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    TreatmentViewDTO update(TreatmentUpdateDTO dto);

    /**
     * Deletes a treatment and all its associated medicines by ID.
     *
     * @param id the ID of the treatment to delete.
     * @throws EntityNotFoundException if the treatment is not found.
     * @throws InvalidDTOException if the ID is null.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    void delete(Long id);

    /**
     * Retrieves a treatment by ID.
     * Access for patients should be further restricted in the implementation to only their own records.
     *
     * @param id the ID of the treatment.
     * @return the treatment's view DTO.
     * @throws EntityNotFoundException if the treatment is not found.
     * @throws InvalidDTOException if the ID is null.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    TreatmentViewDTO getById(Long id);

    /**
     * Asynchronously retrieves all treatments with pagination and sorting.
     *
     * @param page     the page number (0-based).
     * @param size     the number of items per page.
     * @param orderBy  the field to sort by (e.g., "description").
     * @param ascending whether to sort in ascending order.
     * @return a CompletableFuture containing a page of treatment view DTOs.
     * @throws InvalidDTOException if pagination parameters are invalid.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<TreatmentViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);
}