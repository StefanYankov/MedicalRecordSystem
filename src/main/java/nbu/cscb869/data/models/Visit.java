package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

import java.time.LocalDate;

/**
 * Represents a patient visit to a doctor in the medical record system.
 */
@Getter
@Setter
@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private LocalDate visitDate;

    @Column
    private String treatment;

    @NotNull
    @Column(nullable = false)
    private Boolean sickLeaveIssued = false;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    @OneToOne(mappedBy = "visit")
    private SickLeave sickLeave;
}