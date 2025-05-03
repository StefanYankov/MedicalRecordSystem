package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for managing {@link Medicine} entities with soft delete support.
 */
public interface MedicineRepository extends SoftDeleteRepository<Medicine, Long> {
    /**
     * Retrieves a page of non-deleted medicine records.
     * @param pageable pagination information
     * @return a page of medicine entities where {@code isDeleted = false}
     */
    @Query("SELECT m FROM Medicine m WHERE m.isDeleted = false")
    Page<Medicine> findAllActive(Pageable pageable);
}