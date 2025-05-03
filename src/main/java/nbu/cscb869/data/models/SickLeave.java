package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "sick_leaves", indexes = {
        @Index(columnList = "visit_id"),
        @Index(columnList = "is_deleted")
})
@SQLDelete(sql = "UPDATE sick_leaves SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class SickLeave extends BaseEntity {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = ErrorMessages.DURATION_NOT_NULL)
    @Min(value = ValidationConfig.DURATION_MIN_DAYS, message = ErrorMessages.DURATION_MIN)
    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @OneToOne
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;
}