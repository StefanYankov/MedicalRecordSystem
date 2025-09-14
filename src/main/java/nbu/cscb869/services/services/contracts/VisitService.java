package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
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
 * Provides CRUD operations, scheduling validation, and reporting functionality as per the medical record system requirements.
 */
public interface VisitService {

    /**
     * Creates a new visit based on the provided DTO, validating scheduling and insurance.
     * @param dto the DTO containing visit creation data
     * @return the created visit's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws EntityNotFoundException if the patient, doctor, or diagnosis is not found
     * @throws InvalidInputException if the visit time is booked, outside 9:00–17:00, or insurance is invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    VisitViewDTO create(VisitCreateDTO dto);

    /**
     * Updates an existing visit based on the provided DTO, validating scheduling and insurance.
     * @param dto the DTO containing updated visit data
     * @return the updated visit's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the visit, patient, doctor, or diagnosis is not found
     * @throws InvalidInputException if the visit time is booked, outside 9:00–17:00, or insurance is invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    VisitViewDTO update(VisitUpdateDTO dto);

    /**
     * Soft deletes a visit by ID.
     * @param id the ID of the visit to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the visit is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    void delete(Long id);

    /**
     * Retrieves a visit by ID.
     * @param id the ID of the visit
     * @return the visit's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the visit is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    VisitViewDTO getById(Long id);

    /**
     * Retrieves all visits with pagination, sorting, and optional filtering.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @param filter optional filter string
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<VisitViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves visits by patient with pagination.
     * @param patientId the ID of the patient
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws InvalidDTOException if the patient ID is null
     * @throws EntityNotFoundException if the patient is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    CompletableFuture<Page<VisitViewDTO>> findByPatient(Long patientId, int page, int size);

    /**
     * Retrieves visits by diagnosis with pagination.
     * @param diagnosisId the ID of the diagnosis
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws InvalidDTOException if the diagnosis ID is null
     * @throws EntityNotFoundException if the diagnosis is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<VisitViewDTO>> findByDiagnosis(Long diagnosisId, int page, int size);

    /**
     * Retrieves visits within a date range with pagination.
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws InvalidDTOException if dates are null or invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<VisitViewDTO>> findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * Retrieves visits by doctor and date range with pagination.
     * @param doctorId the ID of the doctor
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws InvalidDTOException if parameters are null or invalid
     * @throws EntityNotFoundException if the doctor is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<VisitViewDTO>> findByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * Retrieves visit counts grouped by doctor.
     * @return a list of DTOs with doctors and their visit counts
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorVisitCountDTO> countVisitsByDoctor();

    /**
     * Retrieves the most frequent diagnoses.
     * @return a list of DTOs with diagnoses and their visit counts
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    List<DiagnosisVisitCountDTO> findMostFrequentDiagnoses();

    /**
     * Retrieves the month with the most sick leaves issued.
     * @return the month (1–12) with the highest sick leave count
     * @throws InvalidInputException if no sick leaves exist
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    int findMonthWithMostSickLeaves();

    /**
     * Retrieves doctors with the most sick leaves issued.
     * @return a list of DTOs with doctors and their sick leave counts
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    List<DoctorSickLeaveCountDTO> findDoctorsWithMostSickLeaves();
}