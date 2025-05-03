package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorCreateDTO {
    @NotNull(message = ErrorMessages.NAME_NOT_NULL)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotNull(message = ErrorMessages.UNIQUE_ID_NOT_BLANK)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX, message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String uniqueIdNumber;

    @NotNull(message = ErrorMessages.GP_NOT_NULL)
    private Boolean isGeneralPractitioner;

    @Size(max = ValidationConfig.SPECIALTY_NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String specialtyName;

    private String imageUrl;
}