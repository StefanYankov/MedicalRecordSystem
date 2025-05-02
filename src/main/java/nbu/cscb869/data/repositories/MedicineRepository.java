package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Treatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineRepository extends SoftDeleteRepository<Medicine, Long> {
    Page<Medicine> findByTreatmentAndIsDeletedFalse(Treatment treatment, Pageable pageable);
}