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
@Table(name = "diagnosis")
@SQLDelete(sql = "UPDATE diagnosis SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Diagnosis extends BaseEntity {
    private String name;
    private String description;

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL)
    private List<Visit> visits;
}