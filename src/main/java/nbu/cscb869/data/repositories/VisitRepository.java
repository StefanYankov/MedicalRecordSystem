package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface VisitRepository extends SoftDeleteRepository<Visit, Long> {
    Page<Visit> findByPatientAndIsDeletedFalse(Patient patient, Pageable pageable);
    Page<Visit> findByDoctorAndIsDeletedFalse(Doctor doctor, Pageable pageable);
    Page<Visit> findByDoctorAndVisitDateBetweenAndIsDeletedFalse(Doctor doctor, LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<Visit> findByDiagnosisAndIsDeletedFalse(Diagnosis diagnosis, Pageable pageable);
    Page<Visit> findByVisitDateBetweenAndIsDeletedFalse(LocalDate startDate, LocalDate endDate, Pageable pageable);
}