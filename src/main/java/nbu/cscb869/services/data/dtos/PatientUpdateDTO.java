package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.common.validation.annotations.Egn;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdateDTO {
    @NotNull
    private Long id;

    // This field is for admin use, to re-link a patient to a different keycloak user.
    private String keycloakId;

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @Egn(message = ErrorMessages.EGN_INVALID)
    @NotBlank(message = ErrorMessages.EGN_NOT_BLANK)
    private String egn;

    private LocalDate lastInsurancePaymentDate;

    @NotNull(message = ErrorMessages.GP_NOT_NULL)
    private Long generalPractitionerId;
}