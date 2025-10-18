package nbu.cscb869.services.data.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH, message = ErrorMessages.DESCRIPTION_SIZE)
    private String description;

    @NotNull(message = ErrorMessages.VISIT_ID_NOT_NULL)
    private Long visitId;

    @Valid // Ensures nested validation is triggered
    private List<MedicineUpdateDTO> medicines;
}