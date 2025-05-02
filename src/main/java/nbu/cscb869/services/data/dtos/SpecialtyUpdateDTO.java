package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
public class SpecialtyUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.SPECIALTY_NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;
}