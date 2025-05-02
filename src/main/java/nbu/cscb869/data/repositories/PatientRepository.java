package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends SoftDeleteRepository<Patient, Long> {
    Optional<Patient> findByEgn(String egn);
    Page<Patient> findByGeneralPractitionerAndIsDeletedFalse(Doctor generalPractitioner, Pageable pageable);
    Page<Patient> findByDiagnosis(Diagnosis diagnosis, Pageable pageable);
    @Query("SELECT p FROM Patient p WHERE p.egn = :egn AND p.lastInsurancePaymentDate > CURRENT_DATE")
    Optional<Patient> findByEgnWithValidInsurance(String egn);
}