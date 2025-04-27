package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Patient entities.
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEgn(String egn);

    @Query("SELECT p FROM Patient p WHERE p.id = :id")
    Optional<Patient> findByIdIncludingDeleted(Long id);
}