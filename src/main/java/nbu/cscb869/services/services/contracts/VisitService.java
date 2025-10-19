package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitUpdateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Visit} entities.
 * Provides CRUD operations, scheduling validation, and reporting functionality.
 */
public interface VisitService {

    /**
     * Creates a new visit, validating scheduling and patient insurance status.
     * @param dto the DTO containing visit creation data.
     * @return the created visit's view DTO.
     * @throws InvalidDTOException if the DTO is null or invalid.
     * @throws EntityNotFoundException if the patient, doctor, or diagnosis is not found.
     * @throws InvalidInputException if the visit time is booked or insurance is invalid.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    VisitViewDTO create(VisitCreateDTO dto);

    /**
     * Updates an existing visit, validating scheduling and patient insurance status.
     * @param dto the DTO containing updated visit data.
     * @return the updated visit's view DTO.
     * @throws InvalidDTOException if the DTO or ID is null.
     * @throws EntityNotFoundException if the visit, patient, doctor, or diagnosis is not found.
     * @throws InvalidInputException if the visit time is booked or insurance is invalid.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    VisitViewDTO update(VisitUpdateDTO dto);

    /**
     * Deletes a visit by ID. This will also delete any associated treatment or sick leave.
     * @param id the ID of the visit to delete.
     * @throws InvalidDTOException if the ID is null.
     * @throws EntityNotFoundException if the visit is not found.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    void delete(Long id);

    /**
     * Retrieves a visit by ID.
     * @param id the ID of the visit.
     * @return the visit's view DTO.
     * @throws InvalidDTOException if the ID is null.
     * @throws EntityNotFoundException if the visit is not found.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    VisitViewDTO getById(Long id);

    /**
     * Asynchronously retrieves all visits with pagination, sorting, and optional filtering.
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @param orderBy the field to sort by.
     * @param ascending whether to sort in ascending order.
     * @param filter optional filter string for patient EGN or doctor Unique ID.
     * @return a CompletableFuture containing a page of visit view DTOs.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<VisitViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves a paginated list of visits for a specific patient.
     * @param patientId the ID of the patient.
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @return a page of visit view DTOs.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    Page<VisitViewDTO> getVisitsByPatient(Long patientId, int page, int size);

    /**
     * Retrieves a paginated list of visits for a specific diagnosis.
     * @param diagnosisId the ID of the diagnosis.
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @return a page of visit view DTOs.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<VisitViewDTO> getVisitsByDiagnosis(Long diagnosisId, int page, int size);

    /**
     * Retrieves a paginated list of all visits within a given date range.
     * @param startDate the start date (inclusive).
     * @param endDate the end date (inclusive).
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @return a page of visit view DTOs.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<VisitViewDTO> getVisitsByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * Retrieves a paginated list of visits for a specific doctor within a given date range.
     * @param doctorId the ID of the doctor.
     * @param startDate the start date (inclusive).
     * @param endDate the end date (inclusive).
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @return a page of visit view DTOs.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<VisitViewDTO> getVisitsByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * Retrieves visit counts grouped by doctor.
     * @return a list of DTOs with doctors and their visit counts.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorVisitCountDTO> getVisitCountByDoctor();

    /**
     * Retrieves the most frequent diagnoses based on visit counts.
     * @return a list of DTOs with diagnoses and their visit counts.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DiagnosisVisitCountDTO> getMostFrequentDiagnoses();
}