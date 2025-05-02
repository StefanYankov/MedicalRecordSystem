package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
public class TreatmentUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @Size(max = ValidationConfig.INSTRUCTIONS_MAX_LENGTH,
            message = ErrorMessages.INSTRUCTIONS_SIZE)
    private String instructions;

    private Long visitId;
}