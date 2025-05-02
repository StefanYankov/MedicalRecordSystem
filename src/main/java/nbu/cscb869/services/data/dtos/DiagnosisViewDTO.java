package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisViewDTO {
    private Long id;
    private String name;
    private String description;
}