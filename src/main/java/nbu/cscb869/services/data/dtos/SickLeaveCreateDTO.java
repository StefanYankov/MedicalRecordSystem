package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SickLeaveCreateDTO {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    private LocalDate startDate;

    @NotNull(message = ErrorMessages.DURATION_NOT_NULL)
    @Min(value = ValidationConfig.DURATION_MIN_DAYS, message = ErrorMessages.DURATION_MIN)
    private Integer durationDays;

    @NotNull(message = ErrorMessages.VISIT_ID_NOT_NULL)
    private Long visitId;
}