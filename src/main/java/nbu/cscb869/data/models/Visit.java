package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.data.base.BaseEntity;
import nbu.cscb869.data.models.enums.VisitStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "visits", indexes = {
        @Index(columnList = "patient_id"),
        @Index(columnList = "doctor_id"),
        @Index(columnList = "diagnosis_id"),
        @Index(columnList = "visit_date")
})
@NoArgsConstructor
@AllArgsConstructor
public class Visit extends BaseEntity {

    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.TIME_NOT_NULL)
    @Column(name = "visit_time")
    private LocalTime visitTime;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "diagnosis_id")
    private Diagnosis diagnosis;

    @OneToOne(mappedBy = "visit", cascade = CascadeType.ALL, optional = true)
    private SickLeave sickLeave;

    @OneToOne(mappedBy = "visit", cascade = CascadeType.ALL, optional = true)
    private Treatment treatment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VisitStatus status;

    @Lob
    @Column(name = "notes")
    private String notes;

    public boolean isSickLeaveIssued() {
        return sickLeave != null;
    }
}
