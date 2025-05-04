package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentUpdateDTO {
    private Long id;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH)
    private String description;

    private Long visitId;

    private List<Long> medicineIds;
}