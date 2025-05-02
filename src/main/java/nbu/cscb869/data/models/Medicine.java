package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "medicine")
@SQLDelete(sql = "UPDATE medicine SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Medicine extends BaseEntity {
    private String name;
    private String dosage;
    private String frequency;

    @ManyToOne
    @JoinColumn(name = "treatment_id")
    private Treatment treatment;
}