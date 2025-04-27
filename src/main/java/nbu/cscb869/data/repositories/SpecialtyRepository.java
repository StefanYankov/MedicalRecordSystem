package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Specialty entities.
 */
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    Optional<Specialty> findByName(String name);

    @Query("SELECT s FROM Specialty s WHERE s.id = :id")
    Optional<Specialty> findByIdIncludingDeleted(Long id);
}