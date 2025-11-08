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
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing {@link nbu.cscb869.data.models.Doctor} entities.
 * Provides CRUD operations and doctor-related queries.
 */
public interface DoctorService {

    /**
     * Creates a new doctor from a DTO, without an image.
     *
     * @param dto The DTO containing the new doctor's information.
     * @return The created doctor's view DTO.
     */
    DoctorViewDTO createDoctor(DoctorCreateDTO dto);

    /**
     * Updates an existing doctor from a DTO, without an image.
     *
     * @param dto The DTO containing the updated doctor's information.
     * @return The updated doctor's view DTO.
     */
    DoctorViewDTO updateDoctor(DoctorUpdateDTO dto);

    /**
     * Creates a new doctor with the provided DTO and optional image.
     * @param dto the DTO containing doctor creation data
     * @param image optional image file for the doctor
     * @return the created doctor's view DTO
     * @throws InvalidDTOException if the DTO is null or invalid
     * @throws InvalidDoctorException if the unique ID number already exists
     * @throws EntityNotFoundException if specialties are not found
     * @throws InvalidInputException if image upload fails
     * @deprecated Use {@link #createDoctor(DoctorCreateDTO)} and handle image separately.
     */
    @Deprecated
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
     * @deprecated Use {@link #updateDoctor(DoctorUpdateDTO)} and handle image separately.
     */
    @Deprecated
    DoctorViewDTO update(DoctorUpdateDTO dto, MultipartFile image);

    /**
     * Deletes a doctor by ID.
     * @param id the ID of the doctor to delete
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the doctor is not found
     * @throws InvalidDoctorException if the doctor is a general practitioner with active patients
     */
    void delete(Long id);

    /**
     * Retrieves a doctor by ID.
     * @param id the ID of the doctor
     * @return the doctor's view DTO
     * @throws InvalidDTOException if the ID is null
     * @throws EntityNotFoundException if the doctor is not found
     */
    DoctorViewDTO getById(Long id);

    /**
     * Retrieves a doctor by unique ID number.
     * @param uniqueIdNumber the doctor's unique ID number
     * @return the doctor's view DTO
     * @throws InvalidDTOException if the unique ID number is null or empty
     * @throws EntityNotFoundException if the doctor is not found
     */
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
    @Async
    CompletableFuture<Page<DoctorViewDTO>> getAllAsync(int page, int size, String orderBy, boolean ascending, String filter);

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
    Page<DoctorViewDTO> findByCriteria(Specification<Doctor> spec, int page, int size, String orderBy, boolean ascending);

    /**
     * Finds all doctors belonging to a specific specialty.
     *
     * @param specialtyId The ID of the specialty.
     * @param page        The page number (0-based).
     * @param size        The number of items per page.
     * @param sortBy      The field to sort by.
     * @param asc         Whether to sort in ascending order.
     * @return A {@link Page} of {@link DoctorViewDTO} objects.
     */
    Page<DoctorViewDTO> findAllBySpecialty(Long specialtyId, int page, int size, String sortBy, boolean asc);

    /**
     * Retrieves patients assigned to a specific general practitioner with pagination.
     * @param generalPractitionerId the ID of the general practitioner
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return a page of patient view DTOs
     * @throws InvalidDTOException if the general practitioner ID is null
     * @throws EntityNotFoundException if the general practitioner is not found.
     * @throws InvalidDoctorException if the doctor is not a general practitioner.
     */
    Page<PatientViewDTO> getPatientsByGeneralPractitioner(Long generalPractitionerId, int page, int size);

    /**
     * Retrieves patient counts for general practitioners.
     * @return a list of DTOs containing general practitioners and their patient counts.
     */
    List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner();

    /**
     * Retrieves visit counts for doctors.
     * @return a list of DTOs containing doctors and their visit counts.
     */
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
     * @throws EntityNotFoundException if the doctor is not found.
     */
    Page<VisitViewDTO> getVisitsByPeriod(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size);
    /**
     * Retrieves doctors with the highest sick leave counts.
     * @return a list of DTOs containing doctors and their sick leave counts.
     */
    List<DoctorSickLeaveCountDTO> getDoctorsWithMostSickLeaves();

    /**
     * Retrieves a doctor by their Keycloak user ID.
     * @param keycloakId the user's unique Keycloak ID (sub)
     * @return the doctor's view DTO
     * @throws InvalidInputException if the keycloakId is null or blank
     * @throws EntityNotFoundException if the doctor is not found.
     */
    DoctorViewDTO getByKeycloakId(String keycloakId);

    /**
     * Deletes the profile image for a specific doctor.
     * @param doctorId the ID of the doctor whose image should be deleted.
     */
    void deleteDoctorImage(Long doctorId);

    /**
     * Retrieves a paginated list of unapproved doctors.
     * @param page the page number (0-based).
     * @param size the number of items per page.
     * @return a page of DoctorViewDTOs for unapproved doctors.
     */
    Page<DoctorViewDTO> getUnapprovedDoctors(int page, int size);

    /**
     * Approves a doctor by setting their isApproved flag to true and assigning the DOCTOR role in Keycloak.
     * @param doctorId The ID of the doctor to approve.
     * @throws EntityNotFoundException if the doctor is not found.
     * @throws InvalidDoctorException if the doctor is already approved.
     */
    void approveDoctor(Long doctorId);
}
