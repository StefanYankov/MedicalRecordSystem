package nbu.cscb869.services.data.dtos.identity;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateDTO {
    @NotBlank(message = ErrorMessages.ROLE_NAME_NOT_BLANK)
    private String name;
}