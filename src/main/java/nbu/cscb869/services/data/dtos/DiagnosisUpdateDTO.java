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
public class DiagnosisUpdateDTO {

    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @NotNull(message = ErrorMessages.DIAGNOSIS_NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.DIAGNOSIS_NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotNull(message = ErrorMessages.DESCRIPTION_NOT_BLANK)
    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH, message = ErrorMessages.DESCRIPTION_SIZE)
    private String description;
}