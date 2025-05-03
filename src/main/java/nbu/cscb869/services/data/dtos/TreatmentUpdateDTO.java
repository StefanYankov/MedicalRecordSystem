package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH, message = ErrorMessages.DESCRIPTION_SIZE)
    private String description;

    @NotNull(message = ErrorMessages.VISIT_ID_NOT_NULL)
    private Long visitId;
}