package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
public class DiagnosisCreateDTO {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.DIAGNOSIS_NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH,
            message = ErrorMessages.DESCRIPTION_SIZE)
    private String description;
}