package nbu.cscb869.services.data.dtos.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignmentCreateDTO {
    @NotNull(message = ErrorMessages.USER_NOT_NULL)
    private Long userId;

    @NotNull(message = ErrorMessages.ROLE_NOT_NULL)
    private Long roleId;

    @NotBlank(message = ErrorMessages.ENTITY_TYPE_NOT_BLANK)
    private String entityType;

    @NotNull(message = ErrorMessages.ENTITY_ID_NOT_NULL)
    private Long entityId;
}