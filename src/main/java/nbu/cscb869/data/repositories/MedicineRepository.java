package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    /**
     * Retrieves a page of  medicine records.
     * @param pageable pagination information
     * @return a page of medicine entities
     */
    Page<Medicine> findAll(Pageable pageable);
}