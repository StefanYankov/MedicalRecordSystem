package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for managing {@link Visit} entities with soft delete support.
 */
public interface VisitRepository extends SoftDeleteRepository<Visit, Long> {
    /**
     * Retrieves a page of non-deleted visits for a specific patient.
     *
     * @param patient  the patient whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities where {@code isDeleted = false}
     */
    @Query("SELECT v FROM Visit v WHERE v.patient = :patient AND v.isDeleted = false")
    Page<Visit> findByPatient(Patient patient, Pageable pageable);

    /**
     * Retrieves a page of non-deleted visits for a specific doctor.
     *
     * @param doctor   the doctor whose visits are to be retrieved
     * @param pageable pagination information
     * @return a page of visit entities where {@code isDeleted = false}
     */
    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.isDeleted = false")
    Page<Visit> findByDoctor(Doctor doctor, Pageable pageable);

    /**
     * Retrieves a page of non-deleted visits within a specified date range.
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate   the end date of the range (inclusive)
     * @param pageable  pagination information
     * @return a page of visit entities where {@code isDeleted = false}
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate BETWEEN :startDate AND :endDate AND v.isDeleted = false")
    Page<Visit> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Retrieves a page of non-deleted visits for a specific doctor within a specified date range.
     *
     * @param doctor    the doctor whose visits are to be retrieved
     * @param startDate the start date of the range (inclusive)
     * @param endDate   the end date of the range (inclusive)
     * @param pageable  pagination information
     * @return a page of visit entities where {@code isDeleted = false}
     */
    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.visitDate BETWEEN :startDate AND :endDate " +
            "AND v.isDeleted = false")
    Page<Visit> findByDoctorAndDateRange(Doctor doctor, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Retrieves a page of non-deleted visits for a specific diagnosis.
     *
     * @param diagnosis the diagnosis to filter by
     * @param pageable  pagination information
     * @return a page of visit entities where {@code isDeleted = false}
     */
    @Query("SELECT v FROM Visit v WHERE v.diagnosis = :diagnosis AND v.isDeleted = false")
    Page<Visit> findByDiagnosis(Diagnosis diagnosis, Pageable pageable);

    /**
     * Retrieves a list of diagnoses with their visit counts, sorted by count descending.
     *
     * @return a list of DTOs with diagnosis and visit count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DiagnosisVisitCountDTO(v.diagnosis, COUNT(v)) " +
            "FROM Visit v WHERE v.isDeleted = false " +
            "GROUP BY v.diagnosis ORDER BY COUNT(v) DESC")
    List<DiagnosisVisitCountDTO> findMostFrequentDiagnoses();

    /**
     * Retrieves a list of doctors with their visit counts, sorted by count descending.
     *
     * @return a list of DTOs with doctor and visit count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorVisitCountDTO(v.doctor, COUNT(v)) " +
            "FROM Visit v WHERE v.isDeleted = false " +
            "GROUP BY v.doctor ORDER BY COUNT(v) DESC")
    List<DoctorVisitCountDTO> countVisitsByDoctor();
}