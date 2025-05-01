package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

import java.time.LocalDate;

/**
 * Represents a sick leave issued during a patient visit.
 */
@Getter
@Setter
@Entity
@Table(name = "sick_leaves")
public class SickLeave extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @Positive
    @Column(nullable = false)
    private Integer durationDays;

    @NotNull
    @OneToOne(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;
}