package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Diagnosis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiagnosisRepository extends SoftDeleteRepository<Diagnosis, Long> {
    Optional<Diagnosis> findByName(String name);
    @Query("SELECT COUNT(DISTINCT p) FROM Patient p JOIN Visit v ON v.patient = p WHERE v.diagnosis = :diagnosis AND v.isDeleted = false")
    long countPatientsByDiagnosis(Diagnosis diagnosis);
    @Query("SELECT d, COUNT(v) FROM Diagnosis d JOIN Visit v ON v.diagnosis = d WHERE v.isDeleted = false GROUP BY d ORDER BY COUNT(v) DESC")
    Page<Object[]> findMostFrequentDiagnoses(Pageable pageable);
}