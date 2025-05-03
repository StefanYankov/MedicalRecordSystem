package nbu.cscb869.data.models.base;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base entity providing common fields and soft delete functionality.
 * Includes auditing fields (created/modified) and soft delete fields ({@code isDeleted}, {@code deletedOn}).
 * Entities extending this class should add an index on {@code is_deleted} for query performance.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "is_deleted = false")
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private LocalDateTime createdOn;

    @Column
    private LocalDateTime modifiedOn;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column
    private String modifiedBy;

    @NotNull
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_on")
    private LocalDateTime deletedOn;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        this.createdOn = LocalDateTime.now();
        this.isDeleted = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedOn = LocalDateTime.now();
    }

    // NB! added isDeleted for the methods below

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(isDeleted, that.isDeleted);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, isDeleted);
    }
}