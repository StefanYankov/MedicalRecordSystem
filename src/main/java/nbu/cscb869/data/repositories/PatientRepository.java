package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for managing {@link Patient} entities with soft delete support.
 */
public interface PatientRepository extends SoftDeleteRepository<Patient, Long> {
    /**
     * Finds a patient by EGN, respecting soft delete.
     * @param egn the patient's EGN
     * @return an optional containing the patient if found and not deleted
     */
    Optional<Patient> findByEgn(String egn);

    /**
     * Finds patients assigned to a specific general practitioner.
     * @param generalPractitioner the general practitioner
     * @param pageable pagination information
     * @return a page of patients where {@code isDeleted = false}
     */
    @Query("SELECT p FROM Patient p WHERE p.generalPractitioner = :generalPractitioner AND p.isDeleted = false")
    Page<Patient> findByGeneralPractitioner(Doctor generalPractitioner, Pageable pageable);
}