package nbu.cscb869.services.data.dtos.identity;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserViewDTO {
    private Long id;

    private String keycloakId;

    private String name;

    private String email;

    private Set<RoleViewDTO> roles;
}