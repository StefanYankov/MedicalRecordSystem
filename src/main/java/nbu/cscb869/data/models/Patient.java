package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.common.validation.annotations.Egn;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "patients", indexes = {
        @Index(columnList = "egn"),
        @Index(columnList = "is_deleted")
})
@SQLDelete(sql = "UPDATE patients SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Patient extends BaseEntity {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH)
    @Column(nullable = false)
    private String name;

    @Egn(message = ErrorMessages.EGN_INVALID)
    @NotBlank(message = ErrorMessages.EGN_NOT_BLANK)
    @Column(nullable = false, unique = true)
    private String egn;

    @Column(name = "last_insurance_payment_date")
    private LocalDate lastInsurancePaymentDate;

    @ManyToOne
    @NotNull(message = ErrorMessages.GP_NOT_NULL)
    @JoinColumn(name = "general_practitioner_id")
    private Doctor generalPractitioner;

    @OneToMany(mappedBy = "patient")
    private List<Visit> visits = new ArrayList<>();

    public boolean hasValidInsurance() {
        return lastInsurancePaymentDate != null &&
                lastInsurancePaymentDate.isAfter(LocalDate.now().minusMonths(6));
    }
}