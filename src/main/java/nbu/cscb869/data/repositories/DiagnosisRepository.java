package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Diagnosis entities.
 */
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    Optional<Diagnosis> findByName(String name);

    @Query("SELECT d FROM Diagnosis d WHERE d.id = :id")
    Optional<Diagnosis> findByIdIncludingDeleted(Long id);
}