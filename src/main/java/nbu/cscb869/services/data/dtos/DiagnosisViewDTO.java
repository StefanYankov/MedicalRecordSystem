package nbu.cscb869.services.data.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisViewDTO {
    private Long id;

    private String name;

    private String description;
}