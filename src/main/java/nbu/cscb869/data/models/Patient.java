package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "patient")
@SQLDelete(sql = "UPDATE patient SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Patient extends BaseEntity {
    private String name;
    private String egn;
    private LocalDate lastInsurancePaymentDate;

    @ManyToOne
    @JoinColumn(name = "general_practitioner_id")
    private Doctor generalPractitioner;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<Visit> visits;
}