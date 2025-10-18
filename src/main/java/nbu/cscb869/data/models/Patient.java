package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
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
        @Index(columnList = "keycloak_id"),
        @Index(columnList = "egn")
})
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String keycloakId;

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(
            min = ValidationConfig.NAME_MIN_LENGTH,
            max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
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

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Visit> visits = new ArrayList<>();
}