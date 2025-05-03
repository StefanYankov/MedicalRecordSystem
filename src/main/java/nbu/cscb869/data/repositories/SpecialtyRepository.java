package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for managing {@link Specialty} entities with soft delete support.
 */
public interface SpecialtyRepository extends SoftDeleteRepository<Specialty, Long> {
    /**
     * Finds a specialty by name, respecting soft delete.
     * @param name the specialty name
     * @return an optional containing the specialty if found and not deleted
     */
    @Query("SELECT s FROM Specialty s WHERE s.name = :name AND s.isDeleted = false")
    Optional<Specialty> findByName(String name);

    /**
     * Retrieves all active (non-deleted) specialties with pagination.
     *
     * @param pageable the pagination information (e.g., page number, size)
     * @return a Page containing active specialties
     */
    @Query("SELECT s FROM Specialty s WHERE s.isDeleted = false")
    Page<Specialty> findAllActive(Pageable pageable);
}