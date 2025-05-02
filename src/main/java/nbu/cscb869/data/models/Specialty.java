package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "specialty")
@SQLDelete(sql = "UPDATE specialty SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Specialty extends BaseEntity {
    private String name;

    @ManyToMany(mappedBy = "specialties")
    private Set<Doctor> doctors;
}