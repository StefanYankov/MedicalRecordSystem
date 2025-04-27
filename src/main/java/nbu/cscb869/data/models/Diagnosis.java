package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a medical diagnosis in the medical record system.
 */
@Getter
@Setter
@Entity
@Table(name = "diagnoses")
public class Diagnosis extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "diagnosis")
    private Set<Visit> visits = new HashSet<>();
}