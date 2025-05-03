package nbu.cscb869.data.repositories.base;

import nbu.cscb869.data.models.base.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Base repository interface for entities with soft delete functionality.
 * Provides common methods to retrieve non-deleted entities, ensuring
 * {@code isDeleted = false} is applied automatically via {@code @Where}.
 * Includes an explicit hard delete method for permanent record removal.
 *
 * @param <T> the entity type extending {@link BaseEntity}
 * @param <ID> the type of the entity's ID
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

    /**
     * Retrieves all non-deleted entities.
     * @return a list of entities where {@code isDeleted = false}
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.isDeleted = false")
    List<T> findAllActive();

    /**
     * Retrieves a page of non-deleted entities.
     * @param pageable pagination information
     * @return a page of entities where {@code isDeleted = false}
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.isDeleted = false")
    Page<T> findAllActive(Pageable pageable);

    /**
     * Permanently deletes an entity by ID, bypassing soft delete.
     * @param id the ID of the entity to delete
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id")
    void hardDeleteById(ID id);
}