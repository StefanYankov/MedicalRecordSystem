package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.Set;

@Data
public class DoctorCreateDTO {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotBlank(message = ErrorMessages.DOCTOR_UNIQUE_ID_INVALID_FORMAT)
    @Size(min = ValidationConfig.UNIQUE_ID_MIN_LENGTH, max = ValidationConfig.UNIQUE_ID_MAX_LENGTH, message = ErrorMessages.DOCTOR_UNIQUE_ID_INVALID_FORMAT)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX, message = ErrorMessages.DOCTOR_UNIQUE_ID_INVALID_FORMAT)
    private String uniqueIdNumber;

    private Set<String> specialties;

    private Boolean isGeneralPractitioner;

    private Boolean isApproved;

    private String keycloakId;
}