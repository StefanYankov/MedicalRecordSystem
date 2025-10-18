package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineUpdateDTO {
    private Long id; // Can be null for new medicines added during an update

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(max = ValidationConfig.NAME_MAX_LENGTH, message = ErrorMessages.NAME_SIZE)
    private String name;

    @NotBlank(message = ErrorMessages.DOSAGE_NOT_BLANK)
    @Size(max = ValidationConfig.DOSAGE_MAX_LENGTH, message = ErrorMessages.DOSAGE_SIZE)
    private String dosage;

    @NotBlank(message = ErrorMessages.FREQUENCY_NOT_BLANK)
    @Size(max = ValidationConfig.FREQUENCY_MAX_LENGTH, message = ErrorMessages.FREQUENCY_SIZE)
    private String frequency;
}