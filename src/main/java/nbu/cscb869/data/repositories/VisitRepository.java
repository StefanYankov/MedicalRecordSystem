package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.dto.MonthSickLeaveCountDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.enums.VisitStatus;
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
 * Repository for managing {@link Visit} entities.
 */
public interface VisitRepository extends JpaRepository<Visit, Long> {

    /**
     * Retrieves a single Visit by its ID, eagerly fetching all its child relationships
     * to avoid lazy loading issues.
     *
     * @param id The ID of the visit.
     * @return An Optional containing the fully initialized Visit entity.
     */
    @Query("SELECT v FROM Visit v LEFT JOIN FETCH v.treatment t LEFT JOIN FETCH t.medicines LEFT JOIN FETCH v.sickLeave WHERE v.id = :id")
    Optional<Visit> findByIdWithChildren(@Param("id") Long id);

    /**
     * Retrieves a page of visits for a specific patient.
     * @param patient the patient whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByPatient(Patient patient, Pageable pageable);

    /**
     * Retrieves a page of all visits for a specific patient, ordered by date and time descending.
     * @param patient the patient whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByPatientOrderByVisitDateDescVisitTimeDesc(Patient patient, Pageable pageable);

    /**
     * Retrieves a page of visits for a specific doctor.
     * @param doctor the doctor whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByDoctor(Doctor doctor, Pageable pageable);

    /**
     * Retrieves a list of visits for a specific doctor by their ID.
     * @param doctorId the ID of the doctor whose visits are to be retrieved
     * @return a list of visit entities
     */
    List<Visit> findByDoctorId(Long doctorId);

    /**
     * Checks if a visit exists for a given patient and doctor.
     * @param patientId The ID of the patient.
     * @param doctorId The ID of the doctor.
     * @return true if a visit exists, false otherwise.
     */
    boolean existsByPatientIdAndDoctorId(Long patientId, Long doctorId);

    /**
     * Retrieves a page of visits for a specific doctor with a given status and within a date range.
     * @param doctor the doctor whose visits are to be retrieved
     * @param status the status of the visits to retrieve
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @param pageable pagination information
     * @return a page of visit entities
     */
    Page<Visit> findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(Doctor doctor, VisitStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable);

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
    @Query("SELECT new nbu.cscb869.data.dto.DiagnosisVisitCountDTO(v.diagnosis.id, v.diagnosis.name, COUNT(v)) " +
            "FROM Visit v GROUP BY v.diagnosis.id, v.diagnosis.name ORDER BY COUNT(v) DESC")
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
     * Retrieves a visit by doctor, date, and time.
     * @param doctor the doctor associated with the visit
     * @param visitDate the date of the visit
     * @param visitTime the time of the visit
     * @return an optional containing the visit if found
     */
    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.visitDate = :visitDate AND v.visitTime = :visitTime")
    Optional<Visit> findByDoctorAndDateTime(@Param("doctor") Doctor doctor,
                                            @Param("visitDate") LocalDate visitDate,
                                            @Param("visitTime") LocalTime visitTime);

    /**
     * Retrieves the month with the most issued sick leaves.
     * @return a list of DTOs with the month and the count of sick leaves.
     */
    @Query("SELECT new nbu.cscb869.data.dto.MonthSickLeaveCountDTO(MONTH(sl.startDate), COUNT(sl)) " +
            "FROM SickLeave sl GROUP BY MONTH(sl.startDate) ORDER BY COUNT(sl) DESC")
    List<MonthSickLeaveCountDTO> findMostFrequentSickLeaveMonth();
}
