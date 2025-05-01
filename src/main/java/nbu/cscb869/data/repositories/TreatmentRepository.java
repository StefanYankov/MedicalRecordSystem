package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    @Query("SELECT t FROM Treatment t WHERE t.id = :id")
    Optional<Treatment> findByIdIncludingDeleted(Long id);
}