package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.base.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface SoftDeleteRepository<T extends BaseEntity, ID extends Serializable> extends JpaRepository<T, ID> {
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.isDeleted = true, e.deletedOn = CURRENT_TIMESTAMP, e.version = e.version + 1 WHERE e.id = :id")
    void softDeleteById(@Param("id") ID id);

    @Query("SELECT e FROM #{#entityName} e WHERE e.isDeleted = true")
    List<T> findAllDeleted();

    // TODO: Add scheduled cleanup job for GDPR compliance (e.g., hard-delete records older than x months)
}