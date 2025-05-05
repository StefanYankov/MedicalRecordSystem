package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Doctor} entities.
 * Provides CRUD operations and reporting functionality for doctor management as per the medical record system requirements.
 */
public interface DoctorService {

    /**
     * Creates a new doctor based on the provided DTO.
     *
     * @param dto the DTO containing doctor creation data (name, unique ID, specialties, etc.)
     * @return the created doctor's view DTO
     * @throws InvalidDoctorException if the unique ID already exists or specialties are invalid
     * @throws InvalidDTOException if the DTO is null or contains invalid data
     * @throws InvalidInputException if an unexpected error occurs during creation
     */
    // TODO: Restrict to ROLE_ADMIN once Spring Security is configured
    DoctorViewDTO create(DoctorCreateDTO dto);

    /**
     * Updates an existing doctor based on the provided DTO.
     *
     * @param dto the DTO containing updated doctor data (ID, name, specialties, etc.)
     * @return the updated doctor's view DTO
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDoctorException if specialties are invalid
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws InvalidInputException if an unexpected error occurs during update
     */
    // TODO: Restrict to ROLE_ADMIN or doctor editing own data once Spring Security is configured
    DoctorViewDTO update(DoctorUpdateDTO dto);

    /**
     * Deletes a doctor by ID (soft delete).
     *
     * @param id the ID of the doctor to delete
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDoctorException if the doctor is a general practitioner for active patients
     * @throws InvalidDTOException if the ID is null
     * @throws InvalidInputException if an unexpected error occurs during deletion
     */
    // TODO: Restrict to ROLE_ADMIN once Spring Security is configured
    void delete(Long id);

    /**
     * Retrieves a doctor by ID.
     *
     * @param id the ID of the doctor to retrieve
     * @return the doctor's view DTO
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDTOException if the ID is null
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    DoctorViewDTO getById(Long id);

    /**
     * Retrieves a doctor by unique ID number.
     *
     * @param uniqueIdNumber the unique ID number of the doctor
     * @return the doctor's view DTO
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDTOException if the unique ID number is null or empty
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    DoctorViewDTO getByUniqueIdNumber(String uniqueIdNumber);

    /**
     * Asynchronously retrieves all doctors with pagination, sorting, and optional filtering.
     *
     * @param page     the page number (0-based)
     * @param size     the number of items per page
     * @param orderBy  the field to sort by (e.g., "name", "uniqueIdNumber")
     * @param ascending whether to sort in ascending order
     * @param filter   optional filter string to match against name or unique ID (case-insensitive)
     * @return a CompletableFuture containing a page of doctor view DTOs
     * @throws InvalidInputException if page or size is invalid or an unexpected error occurs
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    CompletableFuture<Page<DoctorViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves doctors by specified criteria (e.g., name, specialty, isGeneralPractitioner) with pagination.
     *
     * @param conditions a map of field names to values (e.g., {"name": "Smith", "isGeneralPractitioner": true, "specialtyId": 1})
     * @param page       the page number (0-based)
     * @param size       the number of items per page
     * @param orderBy    the field to sort by (e.g., "name", "uniqueIdNumber")
     * @param ascending  whether to sort in ascending order
     * @return a page of doctor view DTOs matching the criteria
     * @throws InvalidInputException if page or size is invalid or an unexpected error occurs
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    Page<DoctorViewDTO> findByCriteria(Map<String, Object> conditions, int page, int size, String orderBy, boolean ascending);

    /**
     * Retrieves all patients registered with a specific general practitioner.
     *
     * @param doctorId the ID of the general practitioner
     * @return a page of patient view DTOs
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDTOException if the doctor ID is null
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    Page<PatientViewDTO> getPatientsByGeneralPractitioner(Long doctorId);

    /**
     * Counts the number of patients registered with a specific general practitioner.
     *
     * @param doctorId the ID of the general practitioner
     * @return a DTO with the doctor and patient count
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDTOException if the doctor ID is null
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    DoctorPatientCountDTO getPatientCountByGeneralPractitioner(Long doctorId);

    /**
     * Counts the number of visits for a specific doctor.
     *
     * @param doctorId the ID of the doctor
     * @return a DTO with the doctor and visit count
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDTOException if the doctor ID is null
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    DoctorVisitCountDTO getVisitCount(Long doctorId);

    /**
     * Asynchronously retrieves all visits for a specific doctor within a given date range.
     *
     * @param doctorId   the ID of the doctor
     * @param startDate  the start date of the period (inclusive)
     * @param endDate    the end date of the period (inclusive)
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDTOException if the doctor ID, start date, or end date is null, or if the date range is invalid
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    CompletableFuture<Page<VisitViewDTO>> getVisitsByPeriod(Long doctorId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves doctors with the highest number of issued sick leaves.
     *
     * @return a list of DTOs with doctors and their sick leave counts, sorted by count descending
     * @throws InvalidInputException if an unexpected error occurs during retrieval
     */
    // TODO: Restrict to ROLE_DOCTOR or ROLE_ADMIN once Spring Security is configured
    List<DoctorSickLeaveCountDTO> getDoctorsWithMostSickLeaves();
}