package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.SickLeave} entities.
 * Provides CRUD operations for sick leave management as per the medical record system requirements.
 */
public interface SickLeaveService {

    /**
     * Creates a new sick leave based on the provided DTO.
     * @param dto the DTO containing sick leave creation data
     * @return the created sick leave's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws EntityNotFoundException if the associated visit is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    SickLeaveViewDTO create(SickLeaveCreateDTO dto);

    /**
     * Updates an existing sick leave based on the provided DTO.
     * @param dto the DTO containing updated sick leave data
     * @return the updated sick leave's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the sick leave or visit is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    SickLeaveViewDTO update(SickLeaveUpdateDTO dto);

    /**
     * Deletes a sick leave by ID.
     * @param id the ID of the sick leave to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the sick leave is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    void delete(Long id);

    /**
     * Retrieves a sick leave by ID.
     * Patient access is restricted in the implementation to their own records.
     * @param id the ID of the sick leave
     * @return the sick leave's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the sick leave is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    SickLeaveViewDTO getById(Long id);

    /**
     * Retrieves all sick leaves with pagination and sorting.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @return a CompletableFuture containing a page of sick leave view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<SickLeaveViewDTO>> getAll(int page, int size, String orderBy, boolean ascending);

    /**
     * Retrieves the months with the highest number of issued sick leaves.
     * @return a list of DTOs containing the year, month, and the count of sick leaves.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<YearMonthSickLeaveCountDTO> getMonthsWithMostSickLeaves();

    /**
     * Retrieves the total count of sick leaves.
     * @return the total number of sick leaves.
     */
    long getTotalSickLeavesCount();
}
