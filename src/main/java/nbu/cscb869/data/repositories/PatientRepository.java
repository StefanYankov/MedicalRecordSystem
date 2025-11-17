package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PatientRepository extends JpaRepository<Patient, Long> {
    /**
     * Retrieves a patient by their EGN (Bulgarian personal ID).
     * @param egn the EGN to search for
     * @return an optional patient entity
     */
    Optional<Patient> findByEgn(String egn);

    /**
     * Retrieves a patient by their Keycloak ID.
     * @param keycloakId the Keycloak ID to search for
     * @return an optional patient entity
     */
    Optional<Patient> findByKeycloakId(String keycloakId);

    /**
     * Retrieves a page of patients assigned to a specific general practitioner.
     * @param generalPractitioner the general practitioner to filter by
     * @param pageable pagination information
     * @return a page of patient entities
     */
    Page<Patient> findByGeneralPractitioner(Doctor generalPractitioner, Pageable pageable);

    /**
     * Retrieves a list of all patients assigned to a specific general practitioner.
     * @param generalPractitioner the general practitioner to filter by
     * @return a list of patient entities
     */
    List<Patient> findByGeneralPractitioner(Doctor generalPractitioner);

    /**
     * Retrieves a list of general practitioners with their patient counts.
     * @return a list of DTOs with doctor and patient count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorPatientCountDTO(p.generalPractitioner, COUNT(p)) " +
            "FROM Patient p GROUP BY p.generalPractitioner")
    List<DoctorPatientCountDTO> countPatientsByGeneralPractitioner();

    /**
     * Retrieves a page of patients whose EGN contains the specified filter string (case-insensitive).
     * @param filter the string to match against EGN (wrapped with % for partial matching)
     * @param pageable pagination and sorting information
     * @return a page of {@link Patient} entities where EGN matches the filter
     */
    Page<Patient> findByEgnContaining(String filter, Pageable pageable);

    /**
     * Retrieves a paginated list of unique patients who have at least one visit with the specified diagnosis.
     * @param diagnosisId The ID of the diagnosis to search for.
     * @param pageable Pagination information.
     * @return A Page of unique {@link Patient} entities.
     */
    @Query("SELECT DISTINCT v.patient FROM Visit v WHERE v.diagnosis.id = :diagnosisId")
    Page<Patient> findByDiagnosis(@Param("diagnosisId") Long diagnosisId, Pageable pageable);
}
