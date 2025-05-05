package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreateDTO {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH)
    private String name;

    @NotBlank(message = ErrorMessages.UNIQUE_ID_NOT_BLANK)
    @Size(min = ValidationConfig.UNIQUE_ID_MIN_LENGTH, max = ValidationConfig.UNIQUE_ID_MAX_LENGTH, message = ErrorMessages.UNIQUE_ID_PATTERN)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX, message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String uniqueIdNumber;

    private boolean isGeneralPractitioner;

    private String imageUrl;

    private Set<Long> specialtyIds;
}