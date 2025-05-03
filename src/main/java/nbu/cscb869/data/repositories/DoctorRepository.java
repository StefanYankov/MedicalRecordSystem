package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Doctor} entities with soft delete support.
 */
public interface DoctorRepository extends SoftDeleteRepository<Doctor, Long> {
    /**
     * Finds a doctor by unique ID number, respecting soft delete.
     * @param uniqueIdNumber the doctor's unique ID number
     * @return an optional containing the doctor if found and not deleted
     */
    @Query("SELECT d FROM Doctor d WHERE d.uniqueIdNumber = :uniqueIdNumber AND d.isDeleted = false")
    Optional<Doctor> findByUniqueIdNumber(String uniqueIdNumber);

    /**
     * Retrieves a page of non-deleted doctor records.
     * @param pageable pagination information
     * @return a page of doctor entities where {@code isDeleted = false}
     */
    @Query("SELECT d FROM Doctor d WHERE d.isDeleted = false")
    Page<Doctor> findAllActive(Pageable pageable);

    /**
     * Counts patients per general practitioner, including those with zero patients.
     * @return a list of DTOs with doctors and their patient counts
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorPatientCountDTO(d, COUNT(p)) " +
            "FROM Doctor d LEFT JOIN Patient p ON p.generalPractitioner = d " +
            "WHERE d.isDeleted = false AND (p.isDeleted = false OR p IS NULL) " +
            "GROUP BY d")
    List<DoctorPatientCountDTO> findPatientCountByGeneralPractitioner();

    /**
     * Counts visits per doctor, including those with zero visits.
     * @return a list of DTOs with doctors and their visit counts
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorVisitCountDTO(d, COUNT(v)) " +
            "FROM Doctor d LEFT JOIN Visit v ON v.doctor = d " +
            "WHERE d.isDeleted = false AND (v.isDeleted = false OR v IS NULL) " +
            "GROUP BY d")
    List<DoctorVisitCountDTO> findVisitCountByDoctor();

    /**
     * Identifies doctors with the highest number of issued sick leaves.
     * @return a list of DTOs with doctors and their sick leave counts, sorted by count descending
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorSickLeaveCountDTO(d, COUNT(sl)) " +
            "FROM Doctor d JOIN Visit v ON v.doctor = d JOIN SickLeave sl ON sl.visit = v " +
            "WHERE d.isDeleted = false AND v.isDeleted = false AND sl.isDeleted = false " +
            "GROUP BY d ORDER BY COUNT(sl) DESC")
    List<DoctorSickLeaveCountDTO> findDoctorsWithMostSickLeaves();
}