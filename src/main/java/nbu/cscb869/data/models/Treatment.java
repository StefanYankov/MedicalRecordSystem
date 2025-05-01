package nbu.cscb869.data.models;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a treatment prescribed during a patient visit, including optional instructions and medications.
 */
@Getter
@Setter
@Entity
@Table(name = "treatments")
public class Treatment extends BaseEntity {
    @Column
    private String instructions;

    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Medicine> medicines = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;
}