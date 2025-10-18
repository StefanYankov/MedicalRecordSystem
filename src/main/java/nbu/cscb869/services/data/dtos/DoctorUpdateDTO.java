package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.Set;

@Data
public class DoctorUpdateDTO {
    private Long id;

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotBlank(message = ErrorMessages.UNIQUE_ID_NOT_BLANK)
    @Size(min = ValidationConfig.UNIQUE_ID_MIN_LENGTH, max = ValidationConfig.UNIQUE_ID_MAX_LENGTH)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX, message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String uniqueIdNumber;

    // TODO: NB! check if long ids is not a better approach
    private Set<String> specialties; // Optional
}