package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Patient} entities with soft delete support.
 */
public interface PatientRepository extends SoftDeleteRepository<Patient, Long> {
    /**
     * Retrieves a patient by their EGN (Bulgarian personal ID).
     *
     * @param egn the EGN to search for
     * @return an optional patient entity
     */
    Optional<Patient> findByEgn(String egn);

    /**
     * Retrieves a page of non-deleted patients assigned to a specific general practitioner.
     *
     * @param generalPractitioner the general practitioner to filter by
     * @param pageable            pagination information
     * @return a page of patient entities where {@code isDeleted = false}
     */
    @Query("SELECT p FROM Patient p WHERE p.generalPractitioner = :generalPractitioner AND p.isDeleted = false")
    Page<Patient> findByGeneralPractitioner(Doctor generalPractitioner, Pageable pageable);

    /**
     * Retrieves a list of general practitioners with their patient counts.
     *
     * @return a list of DTOs with doctor and patient count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorPatientCountDTO(p.generalPractitioner, COUNT(p)) " +
            "FROM Patient p WHERE p.isDeleted = false " +
            "GROUP BY p.generalPractitioner")
    List<DoctorPatientCountDTO> countPatientsByGeneralPractitioner();
}