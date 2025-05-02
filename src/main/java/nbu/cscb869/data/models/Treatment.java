package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "treatment")
@SQLDelete(sql = "UPDATE treatment SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Treatment extends BaseEntity {
    private String instructions;

    @OneToOne
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL)
    private List<Medicine> medicines;
}