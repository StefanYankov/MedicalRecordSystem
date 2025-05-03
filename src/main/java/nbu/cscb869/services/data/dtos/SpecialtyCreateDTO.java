package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyCreateDTO {
    @NotNull(message = ErrorMessages.NAME_NOT_NULL)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.SPECIALTY_NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH, message = ErrorMessages.DESCRIPTION_SIZE)
    private String description;
}