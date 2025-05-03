package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @NotNull(message = ErrorMessages.NAME_NOT_NULL)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotNull(message = ErrorMessages.EGN_NOT_BLANK)
    @Pattern(regexp = ValidationConfig.EGN_REGEX, message = ErrorMessages.EGN_INVALID)
    private String egn;

    private Long generalPractitionerId;

    @PastOrPresent(message = ErrorMessages.DATE_PAST_OR_PRESENT)
    private LocalDate lastInsurancePaymentDate;
}