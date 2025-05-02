package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TreatmentRepository extends SoftDeleteRepository<Treatment, Long> {
    Optional<Treatment> findByVisitAndIsDeletedFalse(Visit visit);
}