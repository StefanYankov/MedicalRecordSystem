package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    /**
     * Finds a specialty by name
     * @param name the specialty name
     * @return an optional containing the specialty if found and not deleted
     */
    Optional<Specialty> findByName(String name);

    /**
     * Retrieves all specialties with pagination.
     *
     * @param pageable the pagination information (e.g., page number, size)
     * @return a Page containing active specialties
     */
    Page<Specialty> findAll(Pageable pageable);
}