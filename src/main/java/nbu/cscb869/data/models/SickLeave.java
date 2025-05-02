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
@Table(name = "sick_leave")
@SQLDelete(sql = "UPDATE sick_leave SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class SickLeave extends BaseEntity {
    private LocalDate startDate;
    private int durationDays;

    @OneToOne
    @JoinColumn(name = "visit_id")
    private Visit visit;
}