package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {
    /**
     * Finds a Doctor by their unique Keycloak ID, eagerly fetching specialties.
     * @param keycloakId the user's unique Keycloak ID (sub)
     * @return an Optional containing the Doctor with initialized specialties if found.
     */
    @EntityGraph(attributePaths = "specialties")
    Optional<Doctor> findByKeycloakId(String keycloakId);

    /**
     * Finds a doctor by unique ID number
     * @param uniqueIdNumber the doctor's unique ID number
     * @return an optional containing the doctor if found
     */
    Optional<Doctor> findByUniqueIdNumber(String uniqueIdNumber);

    /**
     * Finds a doctor by their exact name.
     * @param name the name to search for
     * @return an Optional containing the found doctor, or empty if not found
     */
    Optional<Doctor> findByName(String name);

    /**
     * Retrieves a page of non-deleted doctors by unique ID number containing the filter.
     * @param filter the filter string to match against uniqueIdNumber
     * @param pageable pagination information
     * @return a page of doctor entities
     */
    @Query("SELECT d FROM Doctor d WHERE LOWER(d.uniqueIdNumber) LIKE LOWER(:filter)")
    Page<Doctor> findByUniqueIdNumberContaining(@Param("filter") String filter, Pageable pageable);

    /**
     * Finds doctors matching the given specification.
     * @param spec the specification defining the criteria
     * @param pageable pagination information
     * @return a page of doctor entities matching the criteria
     */
    Page<Doctor> findAll(Specification<Doctor> spec, Pageable pageable);

    /**
     * Retrieves a page of patients assigned to a specific general practitioner.
     * @param generalPractitioner the general practitioner whose patients are to be retrieved
     * @param pageable pagination information
     * @return a page of patient entities
     */
    @Query("SELECT p FROM Patient p WHERE p.generalPractitioner = :generalPractitioner")
    Page<Patient> findPatientsByGeneralPractitioner(@Param("generalPractitioner") Doctor generalPractitioner, Pageable pageable);

    /**
     * Counts patients per general practitioner, including those with zero patients.
     * @return a list of DTOs with doctors and their patient counts
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorPatientCountDTO(d, COUNT(p)) " +
            "FROM Doctor d LEFT JOIN Patient p ON p.generalPractitioner = d " +
            "GROUP BY d")
    List<DoctorPatientCountDTO> findPatientCountByGeneralPractitioner();

    /**
     * Counts visits per doctor, including those with zero visits.
     * @return a list of DTOs with doctors and their visit counts
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorVisitCountDTO(d, COUNT(v)) " +
            "FROM Doctor d LEFT JOIN Visit v ON v.doctor = d " +
            "GROUP BY d")
    List<DoctorVisitCountDTO> findVisitCountByDoctor();

    /**
     * Identifies doctors with the highest number of issued sick leaves.
     * @return a list of DTOs with doctors and their sick leave counts, sorted by count descending
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorSickLeaveCountDTO(d, COUNT(sl)) " +
            "FROM Doctor d JOIN Visit v ON v.doctor = d JOIN SickLeave sl ON sl.visit = v " +
            "GROUP BY d ORDER BY COUNT(sl) DESC")
    List<DoctorSickLeaveCountDTO> findDoctorsWithMostSickLeaves();

    /**
     * Checks if any doctor is associated with the given specialty.
     * @param specialty the specialty to check for
     * @return true if the specialty is in use, false otherwise
     */
    boolean existsBySpecialtiesContains(Specialty specialty);

    /**
     * Finds a doctor by ID and eagerly fetches the associated specialties collection.
     * @param id the ID of the doctor to find.
     * @return an Optional containing the Doctor with initialized specialties if found.
     */
    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.specialties WHERE d.id = :id")
    Optional<Doctor> findByIdWithSpecialties(@Param("id") Long id);

    /**
     * Retrieves a page of doctors based on their approval status.
     * @param isApproved boolean indicating approval status.
     * @param pageable pagination information.
     * @return a page of Doctor entities.
     */
    Page<Doctor> findByIsApproved(boolean isApproved, Pageable pageable);
}
