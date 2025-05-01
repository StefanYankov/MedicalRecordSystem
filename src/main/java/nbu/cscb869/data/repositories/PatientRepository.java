package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Repository for Patient entities.
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEgn(String egn);

    @Query("SELECT p FROM Patient p WHERE p.id = :id")
    Optional<Patient> findByIdIncludingDeleted(Long id);

    @Query("SELECT p FROM Patient p JOIN p.visits v JOIN v.diagnosis d WHERE d.id = :diagnosisId AND p.isDeleted = false")
    Page<Patient> findPatientsByDiagnosisId(Long diagnosisId, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.generalPractitioner.id = :doctorId AND p.isDeleted = false")
    Page<Patient> findPatientsByGeneralPractitionerId(Long doctorId, Pageable pageable);
}