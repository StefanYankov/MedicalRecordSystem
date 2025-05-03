package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "visits", indexes = {
        @Index(columnList = "patient_id"),
        @Index(columnList = "doctor_id"),
        @Index(columnList = "diagnosis_id"),
        @Index(columnList = "is_deleted")
})
@SQLDelete(sql = "UPDATE visits SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Visit extends BaseEntity {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.SICK_LEAVE_NOT_NULL)
    @Column(name = "sick_leave_issued", nullable = false)
    private boolean sickLeaveIssued;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    @OneToOne(mappedBy = "visit")
    private SickLeave sickLeave;

    @OneToOne(mappedBy = "visit")
    private Treatment treatment;
}