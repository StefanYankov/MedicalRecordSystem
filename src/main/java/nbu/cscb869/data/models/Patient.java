package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.annotations.Egn;
import nbu.cscb869.data.base.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@Table(name = "patients", indexes = {
        @Index(columnList = "egn")
})
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends BaseEntity {

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

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Visit> visits = new ArrayList<>();
}