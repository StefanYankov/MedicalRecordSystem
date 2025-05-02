package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "visit")
@SQLDelete(sql = "UPDATE visit SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Visit extends BaseEntity {
    private LocalDate visitDate;
    private boolean sickLeaveIssued;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "diagnosis_id")
    private Diagnosis diagnosis;

    @OneToOne(mappedBy = "visit", cascade = CascadeType.ALL)
    private SickLeave sickLeave;

    @OneToOne(mappedBy = "visit", cascade = CascadeType.ALL)
    private Treatment treatment;
}