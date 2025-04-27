package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a medical specialty.
 */
@Getter
@Setter
@Entity
@Table(name = "specialties")
public class Specialty extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "specialties")
    private Set<Doctor> doctors = new HashSet<>();
}