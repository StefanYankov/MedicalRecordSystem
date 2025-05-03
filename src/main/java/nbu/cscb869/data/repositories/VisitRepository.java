package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

/**
 * Repository for managing {@link Visit} entities with soft delete support.
 */
public interface VisitRepository extends SoftDeleteRepository<Visit, Long> {
    @Query("SELECT v FROM Visit v WHERE v.patient = :patient AND v.isDeleted = false")
    Page<Visit> findByPatient(Patient patient, Pageable pageable);

    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.isDeleted = false")
    Page<Visit> findByDoctor(Doctor doctor, Pageable pageable);

    @Query("SELECT v FROM Visit v WHERE v.visitDate BETWEEN :startDate AND :endDate AND v.isDeleted = false")
    Page<Visit> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT v FROM Visit v WHERE v.doctor = :doctor AND v.visitDate BETWEEN :startDate AND :endDate " +
            "AND v.isDeleted = false")
    Page<Visit> findByDoctorAndDateRange(Doctor doctor, LocalDate startDate, LocalDate endDate, Pageable pageable);
}