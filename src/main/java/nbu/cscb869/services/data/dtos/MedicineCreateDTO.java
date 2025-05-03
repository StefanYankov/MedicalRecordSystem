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
public class MedicineCreateDTO {
    @NotNull(message = ErrorMessages.NAME_NOT_NULL)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotNull(message = ErrorMessages.DOSAGE_NOT_BLANK)
    @Size(max = ValidationConfig.DOSAGE_MAX_LENGTH, message = ErrorMessages.DOSAGE_SIZE)
    private String dosage;

    @NotNull(message = ErrorMessages.FREQUENCY_NOT_BLANK)
    @Size(max = ValidationConfig.FREQUENCY_MAX_LENGTH, message = ErrorMessages.FREQUENCY_SIZE)
    private String frequency;

    @NotNull(message = ErrorMessages.TREATMENT_ID_NOT_NULL)
    private Long treatmentId;
}