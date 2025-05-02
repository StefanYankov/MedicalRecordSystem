package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.Set;

@Getter
@Setter
public class DoctorUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX,
            message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String uniqueIdNumber;

    private Boolean isGeneralPractitioner;

    private Set<String> specialtyNames;
}