package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a patient in the medical record system.
 */
@Getter
@Setter
@Entity
@Table(name = "patients")
public class Patient extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String egn;

    @NotNull
    @Column(nullable = false)
    private Boolean healthInsurancePaid;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "general_practitioner_id", nullable = false)
    private Doctor generalPractitioner;

    @OneToMany(mappedBy = "patient")
    private Set<Visit> visits = new HashSet<>();
}