package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
public class TreatmentCreateDTO {
    @NotBlank(message = ErrorMessages.INSTRUCTIONS_NOT_BLANK)
    @Size(max = ValidationConfig.INSTRUCTIONS_MAX_LENGTH,
            message = ErrorMessages.INSTRUCTIONS_SIZE)
    private String instructions;

    @NotNull(message = ErrorMessages.VISIT_ID_NOT_NULL)
    private Long visitId;
}