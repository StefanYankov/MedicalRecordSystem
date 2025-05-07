package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Diagnosis} entities with soft delete support.
 */
public interface DiagnosisRepository extends SoftDeleteRepository<Diagnosis, Long> {
    /**
     * Finds a diagnosis by name, respecting soft delete.
     * @param name the diagnosis name
     * @return an optional containing the diagnosis if found and not deleted
     */
    @Query("SELECT d FROM Diagnosis d WHERE d.name = :name AND d.isDeleted = false")
    Optional<Diagnosis> findByName(String name);

    /**
     * Finds patients diagnosed with a specific diagnosis.
     * @param diagnosis the diagnosis to filter by
     * @param pageable pagination information
     * @return a page of DTOs with patients and diagnosis names
     */
    @Query("SELECT new nbu.cscb869.data.dto.PatientDiagnosisDTO(p, d.name) " +
            "FROM Patient p JOIN Visit v ON v.patient = p JOIN Diagnosis d ON v.diagnosis = d " +
            "WHERE d = :diagnosis AND p.isDeleted = false AND v.isDeleted = false")
    Page<PatientDiagnosisDTO> findPatientsByDiagnosis(Diagnosis diagnosis, Pageable pageable);

    /**
     * Identifies the most frequently diagnosed conditions.
     * @return a list of DTOs with diagnoses and their visit counts, sorted by count descending
     */
    @Query("SELECT new nbu.cscb869.data.dto.DiagnosisVisitCountDTO(d, COUNT(v)) " +
            "FROM Diagnosis d JOIN Visit v ON v.diagnosis = d " +
            "WHERE d.isDeleted = false AND v.isDeleted = false " +
            "GROUP BY d ORDER BY COUNT(v) DESC")
    List<DiagnosisVisitCountDTO> findMostFrequentDiagnoses();

    /**
     * Finds diagnoses by name containing the specified string, case-insensitive, respecting soft delete.
     * Results are sorted alphabetically by name.
     * @param name the partial name to match (without wildcards)
     * @param pageable pagination information
     * @return a page of diagnoses where name contains the input string and isDeleted = false
     */
    @Query("SELECT d FROM Diagnosis d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) AND d.isDeleted = false ORDER BY d.name")
    Page<Diagnosis> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}