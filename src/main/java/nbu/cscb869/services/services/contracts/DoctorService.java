package nbu.cscb869.services.services.contracts;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Doctor} entities.
 * Provides CRUD operations and doctor-related queries with role-based access control.
 */
public interface DoctorService {

    /**
     * Creates a new doctor with the provided DTO and optional image.
     * @param dto the DTO containing doctor creation data
     * @param image optional image file for the doctor
     * @return the created doctor's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidDoctorException if the unique ID number already exists
     * @throws EntityNotFoundException if specialties are not found
     * @throws InvalidInputException if image upload fails
     */
    @PreAuthorize("hasRole('ADMIN')")
    DoctorViewDTO create(DoctorCreateDTO dto, MultipartFile image);

    /**
     * Updates an existing doctor with the provided DTO and optional image.
     * @param dto the DTO containing updated doctor data
     * @param image optional image file for the doctor
     * @return the updated doctor's view DTO
     * @throws InvalidDTOException if the DTO or ID is null
     * @throws EntityNotFoundException if the doctor or specialties are not found
     * @throws InvalidDoctorException if the unique ID number is already in use
     * @throws InvalidInputException if image upload fails
     */
    @PreAuthorize("hasRole('ADMIN')")
    DoctorViewDTO update(DoctorUpdateDTO dto, MultipartFile image);

    /**
     * Soft deletes a doctor by ID.
     * @param id the ID of the doctor to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDoctorException if the doctor is a general practitioner with active patients
     */
    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);

    /**
     * Retrieves a doctor by ID.
     * @param id the ID of the doctor
     * @return the doctor's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the doctor is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    DoctorViewDTO getById(Long id);

    /**
     * Retrieves a doctor by unique ID number.
     * @param uniqueIdNumber the doctor's unique ID number
     * @return the doctor's view DTO
     * @throws InvalidDTOException if the unique ID number is null or empty
     * @throws EntityNotFoundException if the doctor is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    DoctorViewDTO getByUniqueIdNumber(String uniqueIdNumber);

    /**
     * Retrieves all active doctors with pagination, sorting, and optional filtering by unique ID number.
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @param filter optional filter for unique ID number
     * @return a CompletableFuture containing a page of doctor view DTOs
     * @throws InvalidDTOException if pagination parameters are invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<DoctorViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter);

    /**
     * Retrieves doctors by dynamic criteria with pagination and sorting.
     * @param spec the specification defining the criteria
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @param orderBy the field to sort by
     * @param ascending whether to sort in ascending order
     * @return a page of doctor view DTOs
     * @throws InvalidInputException if pagination parameters are invalid
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<DoctorViewDTO> findByCriteria(Specification<Doctor> spec, int page, int size, String orderBy, boolean ascending);

    /**
     * Retrieves patients assigned to a specific general practitioner with pagination.
     * @param generalPractitionerId the ID of the general practitioner
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a page of patient view DTOs
     * @throws InvalidDTOException if the general practitioner ID is null
     * @throws EntityNotFoundException if the general practitioner is not found
     * @throws InvalidDoctorException if the doctor is not a general practitioner
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size);

    /**
     * Retrieves patient counts for general practitioners.
     * @return a list of DTOs containing general practitioners and their patient counts
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner();

    /**
     * Retrieves visit counts for doctors.
     * @return a list of DTOs containing doctors and their visit counts
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorVisitCountDTO> getVisitCount();

    /**
     * Retrieves visits for a doctor within a date range.
     * @param doctorId the ID of the doctor
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a CompletableFuture containing a page of visit view DTOs
     * @throws InvalidDTOException if parameters are null
     * @throws InvalidInputException if the date range is invalid
     * @throws EntityNotFoundException if the doctor is not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    CompletableFuture<Page<VisitViewDTO>> getVisitsByPeriod(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * Retrieves doctors with the highest sick leave counts.
     * @return a list of DTOs containing doctors and their sick leave counts
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<DoctorSickLeaveCountDTO> getDoctorsWithMostSickLeaves();
}