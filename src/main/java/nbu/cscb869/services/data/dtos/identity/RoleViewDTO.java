package nbu.cscb869.services.data.dtos.identity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleViewDTO {
    private Long id;

    private String name;
}