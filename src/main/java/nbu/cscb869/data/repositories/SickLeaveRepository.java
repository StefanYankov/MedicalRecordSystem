package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.SickLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for SickLeave entities.
 */
public interface SickLeaveRepository extends JpaRepository<SickLeave, Long> {
    @Query("SELECT s FROM SickLeave s WHERE s.id = :id")
    Optional<SickLeave> findByIdIncludingDeleted(Long id);
}