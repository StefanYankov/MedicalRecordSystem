package nbu.cscb869.services.data.dtos.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDTO {
    @NotBlank(message = ErrorMessages.KEYCLOAK_ID_NOT_BLANK)
    private String keycloakId;

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotBlank(message = ErrorMessages.EMAIL_NOT_BLANK)
    @Email(message = ErrorMessages.EMAIL_INVALID)
    @Size(min = ValidationConfig.EMAIL_MIN_LENGTH, max = ValidationConfig.EMAIL_MAX_LENGTH, message = ErrorMessages.EMAIL_SIZE)
    private String email;
}