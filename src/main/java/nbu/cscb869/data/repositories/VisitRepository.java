package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Visit} entities with soft delete support.
 */
public interface VisitRepository extends JpaRepository<Visit, Long> {
    /**
     * Retrieves a page of visits for a specific patient.
     * @param patient the patient whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByPatient(Patient patient, Pageable pageable);

    /**
     * Retrieves a page of visits for a specific doctor.
     * @param doctor the doctor whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByDoctor(Doctor doctor, Pageable pageable);

    /**
     * Retrieves a page of visits within a specified date range.
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @param pageable pagination information
     * @return a page of visit entities
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate BETWEEN :startDate AND :endDate")
    Page<Visit> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Retrieves a page of visits for a specific doctor within a specified date range.
     * @param doctor the doctor whose visits are to be retrieved
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @param pageable pagination information
     * @return a page of visit entities
     */
    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.visitDate BETWEEN :startDate AND :endDate")
    Page<Visit> findByDoctorAndDateRange(@Param("doctor") Doctor doctor,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         Pageable pageable);

    /**
     * Retrieves a page of visits for a specific diagnosis.
     * @param diagnosis the diagnosis to filter by
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByDiagnosis(Diagnosis diagnosis, Pageable pageable);

    /**
     * Retrieves a list of diagnoses with their visit counts, sorted by count descending.
     * @return a list of DTOs with diagnosis and visit count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DiagnosisVisitCountDTO(v.diagnosis, COUNT(v)) " +
            "FROM Visit v GROUP BY v.diagnosis ORDER BY COUNT(v) DESC")
    List<DiagnosisVisitCountDTO> findMostFrequentDiagnoses();

    /**
     * Retrieves a list of doctors with their visit counts, sorted by count descending.
     * @return a list of DTOs with doctor and visit count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorVisitCountDTO(v.doctor, COUNT(v)) " +
            "FROM Visit v GROUP BY v.doctor ORDER BY COUNT(v) DESC")
    List<DoctorVisitCountDTO> countVisitsByDoctor();

    /**
     * Retrieves a page of visits filtered by patient EGN or doctor unique ID number.
     * The filter is treated as a prefix for EGN and a full match for uniqueIdNumber.
     * @param filter the string to match against patient EGN (prefix) or doctor unique ID number
     * @param pageable pagination information
     * @return a page of visit entities
     */
    @Query("SELECT v FROM Visit v JOIN v.patient p JOIN v.doctor d " +
            "WHERE (LOWER(p.egn) LIKE LOWER(CONCAT(:filter, '%')) OR LOWER(d.uniqueIdNumber) LIKE LOWER(:filter))")
    Page<Visit> findByPatientOrDoctorFilter(@Param("filter") String filter, Pageable pageable);

    /**
     * Retrieves a visit by doctor, date, and time, respecting soft delete.
     * @param doctor the doctor associated with the visit
     * @param visitDate the date of the visit
     * @param visitTime the time of the visit
     * @return an optional containing the visit if found and not deleted
     */
    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.visitDate = :visitDate AND v.visitTime = :visitTime")
    Optional<Visit> findByDoctorAndDateTime(@Param("doctor") Doctor doctor,
                                            @Param("visitDate") LocalDate visitDate,
                                            @Param("visitTime") LocalTime visitTime);
}