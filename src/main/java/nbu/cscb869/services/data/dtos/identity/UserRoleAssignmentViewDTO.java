package nbu.cscb869.services.data.dtos.identity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignmentViewDTO {
    private Long id;

    private Long userId;

    private Long roleId;

    private String entityType;

    private Long entityId;
}