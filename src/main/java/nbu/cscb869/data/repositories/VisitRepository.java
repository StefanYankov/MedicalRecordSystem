package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Visit entities.
 */
public interface VisitRepository extends JpaRepository<Visit, Long> {
    @Query("SELECT v FROM Visit v WHERE v.id = :id")
    Optional<Visit> findByIdIncludingDeleted(Long id);
}