package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for managing {@link Treatment} entities with soft delete support.
 */
public interface TreatmentRepository extends SoftDeleteRepository<Treatment, Long> {
    /**
     * Retrieves a page of non-deleted treatment records.
     * @param pageable pagination information
     * @return a page of treatment entities where {@code isDeleted = false}
     */
    @Query("SELECT t FROM Treatment t WHERE t.isDeleted = false")
    Page<Treatment> findAllActive(Pageable pageable);
}