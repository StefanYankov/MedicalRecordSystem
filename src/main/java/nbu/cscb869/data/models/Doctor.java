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
 * Represents a doctor in the medical record system.
 */
@Getter
@Setter
@Entity
@Table(name = "doctors")
public class Doctor extends BaseEntity {
    @NotBlank
    @Column(unique = true, nullable = false)
    private String uniqueIdNumber;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "doctor_specialties", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "specialty")
    private Set<String> specialties = new HashSet<>();

    @NotNull
    @Column(nullable = false)
    private Boolean isGeneralPractitioner = false;

    @OneToMany(mappedBy = "generalPractitioner")
    private Set<Patient> patients = new HashSet<>();

    @OneToMany(mappedBy = "doctor")
    private Set<Visit> visits = new HashSet<>();
}