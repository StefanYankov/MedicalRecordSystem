package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Treatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    /**
     * Retrieves a page of non-deleted treatment records.
     * @param pageable pagination information
     * @return a page of treatment entities
     */
    Page<Treatment> findAll(Pageable pageable);
}