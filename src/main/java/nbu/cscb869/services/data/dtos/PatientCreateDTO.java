package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.common.validation.annotations.Egn;

import java.time.LocalDate;

@Getter
@Setter
public class PatientCreateDTO {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotBlank(message = ErrorMessages.EGN_NOT_BLANK)
    @Egn(message = ErrorMessages.EGN_INVALID)
    private String egn;

    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @PastOrPresent(message = ErrorMessages.DATE_PAST_OR_PRESENT)
    private LocalDate lastInsurancePaymentDate;

    @NotBlank(message = ErrorMessages.GP_ID_NOT_BLANK)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX,
            message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String generalPractitionerUniqueIdNumber;
}