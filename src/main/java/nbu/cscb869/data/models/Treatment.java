package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "treatments", indexes = @Index(columnList = "is_deleted"))
@SQLDelete(sql = "UPDATE treatments SET is_deleted = true WHERE id = ?")
public class Treatment extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Medicine> medicines = new ArrayList<>();
}